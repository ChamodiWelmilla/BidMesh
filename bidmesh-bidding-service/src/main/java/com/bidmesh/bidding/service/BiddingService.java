package com.bidmesh.bidding.service;

import com.bidmesh.bidding.dto.BidHistoryResponse;
import com.bidmesh.bidding.dto.BidRequest;
import com.bidmesh.bidding.dto.BidResponse;
import com.bidmesh.bidding.messaging.BidPlacedEvent;
import com.bidmesh.bidding.model.AuctionProjection;
import com.bidmesh.bidding.model.AuctionStatus;
import com.bidmesh.bidding.model.Bid;
import com.bidmesh.bidding.repository.AuctionRepository;
import com.bidmesh.bidding.repository.BidRepository;
import com.bidmesh.bidding.security.BidderPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingService {
    private static final String AUCTION_CACHE_KEY = "auction:";
    private static final String AUCTION_LOCK_KEY = "auction_lock:";
    private static final String AUCTION_BIDS_TOPIC = "auction-bids";

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public BidResponse placeBid(Long auctionId, BidRequest bidRequest, BidderPrincipal bidder) {
        if (bidRequest.getAmount() == null) {
            throw new IllegalArgumentException("Bid amount is required");
        }

        RLock lock = redissonClient.getLock(AUCTION_LOCK_KEY + auctionId);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Could not acquire lock for auction " + auctionId);
            }

            AuctionProjection auction = getAuctionDetails(auctionId);
            if (auction.isDeleted() || auction.getStatus() != AuctionStatus.ACTIVE) {
                throw new IllegalStateException("Auction is not active");
            }
            if (bidRequest.getAmount().compareTo(auction.getCurrentPrice()) <= 0) {
                throw new IllegalArgumentException(
                        "Bid amount must be higher than current price: "
                                + auction.getCurrentPrice().stripTrailingZeros().toPlainString());
            }

            LocalDateTime bidTime = LocalDateTime.now();
            Bid bid = bidRepository.saveAndFlush(Bid.builder()
                    .auctionId(auctionId)
                    .bidderId(bidder.userId())
                    .bidderUsername(bidder.username())
                    .itemName(auction.getItemName())
                    .amount(bidRequest.getAmount())
                    .bidTime(bidTime)
                    .build());

            auction.setCurrentPrice(bid.getAmount());
            auction.setBidCount(auction.getBidCount() + 1);
            auctionRepository.saveAndFlush(auction);

            BidPlacedEvent event = BidPlacedEvent.builder()
                    .bidId(bid.getId())
                    .auctionId(auctionId)
                    .bidderId(bidder.userId())
                    .bidderUsername(bidder.username())
                    .amount(bid.getAmount())
                    .bidTime(bidTime)
                    .bidCount(auction.getBidCount())
                    .build();
            publishBidEvent(event);
            redisTemplate.delete(AUCTION_CACHE_KEY + auctionId);

            return BidResponse.builder()
                    .id(bid.getId())
                    .amount(bid.getAmount())
                    .bidTime(bidTime)
                    .bidderUsername(bidder.username())
                    .auctionId(auctionId)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring auction lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public List<BidHistoryResponse> getBidHistory(Long bidderId) {
        return bidRepository.findByBidderIdOrderByBidTimeDesc(bidderId).stream()
                .map(bid -> BidHistoryResponse.builder()
                        .bidId(bid.getId())
                        .amount(bid.getAmount())
                        .bidTime(bid.getBidTime())
                        .auctionId(bid.getAuctionId())
                        .itemName(bid.getItemName())
                        .build())
                .toList();
    }

    private AuctionProjection getAuctionDetails(Long id) {
        String key = AUCTION_CACHE_KEY + id;
        Object cachedData = redisTemplate.opsForValue().get(key);
        if (cachedData != null) {
            try {
                return objectMapper.convertValue(cachedData, AuctionProjection.class);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to convert cached auction {}, falling back to database", id, e);
            }
        }

        AuctionProjection auction = auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + id));
        if (auction.isDeleted()) {
            throw new IllegalArgumentException("Auction not found: " + id);
        }
        redisTemplate.opsForValue().set(key, auction, 10, TimeUnit.MINUTES);
        return auction;
    }

    private void publishBidEvent(BidPlacedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(AUCTION_BIDS_TOPIC, event.getAuctionId().toString(), payload)
                    .get(10, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize bid event", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing bid event", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish bid event", e);
        }
    }
}
