package com.bidmesh.bidding.service;

import com.bidmesh.bidding.repository.AuctionRepository;
import com.bidmesh.bidding.repository.UserCredentialRepository;
import com.bidmesh.dto.BidRequest;
import com.bidmesh.dto.BidResponse;
import com.bidmesh.event.BidPlacedEvent;
import com.bidmesh.model.Auction;
import com.bidmesh.model.AuctionStatus;
import com.bidmesh.model.Bid;
import com.bidmesh.model.UserCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingService {

    private final AuctionRepository auctionRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String AUCTION_CACHE_KEY = "auction:";
    private static final String AUCTION_LOCK_KEY = "auction_lock:";
    private static final String AUCTION_BIDS_TOPIC = "auction-bids";

    public BidResponse placeBid(Long auctionId, BidRequest bidRequest, String userEmail) {
        UserCredential credential = userCredentialRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        bidRequest.setBidderId(credential.getUser().getId());

        String lockKey = AUCTION_LOCK_KEY + auctionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                log.info("Lock acquired for auction {}", auctionId);

                Auction auction = getAuctionDetails(auctionId);

                if (auction.getStatus() != AuctionStatus.ACTIVE) {
                    throw new RuntimeException("Auction is not active");
                }
                if (bidRequest.getAmount().compareTo(auction.getCurrentPrice()) <= 0) {
                    throw new RuntimeException("Bid amount must be higher than current price: " + auction.getCurrentPrice().stripTrailingZeros().toPlainString());
                }

                BidPlacedEvent event = BidPlacedEvent.builder()
                        .auctionId(auctionId)
                        .bidderId(bidRequest.getBidderId())
                        .amount(bidRequest.getAmount())
                        .bidTime(LocalDateTime.now())
                        .build();

                // 1. Update Cache Immediately
                auction.setCurrentPrice(bidRequest.getAmount());
                if (auction.getBids() == null) {
                    auction.setBids(new ArrayList<>());
                }
                Bid cachedBid = new Bid();
                cachedBid.setAmount(bidRequest.getAmount());
                auction.getBids().add(cachedBid);
                
                redisTemplate.opsForValue().set(AUCTION_CACHE_KEY + auctionId, auction, 10, TimeUnit.MINUTES);

                // 2. Publish to Kafka (Partitioned by auctionId to ensure ordering)
                kafkaTemplate.send(AUCTION_BIDS_TOPIC, String.valueOf(auctionId), event);

                log.info("Bid event published for auction {}", auctionId);

                return BidResponse.builder()
                        .amount(bidRequest.getAmount())
                        .bidTime(event.getBidTime())
                        .auctionId(auctionId)
                        .build();
            } else {
                throw new RuntimeException("Could not acquire lock for auction " + auctionId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Auction getAuctionDetails(Long id) {
        String key = AUCTION_CACHE_KEY + id;
        Object cachedData = redisTemplate.opsForValue().get(key);

        if (cachedData != null) {
            try {
                return objectMapper.convertValue(cachedData, Auction.class);
            } catch (Exception e) {
                log.warn("Failed to convert cached auction, falling back to DB", e);
            }
        }

        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));
        redisTemplate.opsForValue().set(key, auction, 10, TimeUnit.MINUTES);
        return auction;
    }
}
