package com.bidmesh.service;

import com.bidmesh.dto.BidRequest;
import com.bidmesh.model.Auction;
import com.bidmesh.model.AuctionStatus;
import com.bidmesh.model.Bid;
import com.bidmesh.model.User;
import com.bidmesh.repository.AuctionRepository;
import com.bidmesh.repository.BidRepository;
import com.bidmesh.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final com.bidmesh.repository.ItemRepository itemRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String AUCTION_CACHE_KEY = "auction:";
    private static final String AUCTION_LOCK_KEY = "auction_lock:";

    @Transactional
    public Auction createAuction(Auction auction, Long creatorId, Long itemId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        com.bidmesh.model.Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        auction.setCreator(creator);
        auction.setItem(item);
        auction.setCurrentPrice(auction.getStartPrice());
        auction.setStatus(AuctionStatus.ACTIVE);
        
        return auctionRepository.save(auction);
    }

    /**
     * Cache-Aside Pattern Implementation
     */
    public Auction getAuctionDetails(Long id) {
        String key = AUCTION_CACHE_KEY + id;
        Object cachedData = redisTemplate.opsForValue().get(key);

        if (cachedData != null) {
            try {
                log.info("Auction {} retrieved from Redis cache", id);
                return objectMapper.convertValue(cachedData, Auction.class);
            } catch (Exception e) {
                log.warn("Failed to convert cached data for auction {}, falling back to DB", id, e);
            }
        }

        log.info("Auction {} not in cache, fetching from PostgreSQL", id);
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        redisTemplate.opsForValue().set(key, auction, 10, TimeUnit.MINUTES);
        return auction;
    }

    public java.util.List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    private static final String AUCTION_BIDS_TOPIC = "auction-bids";

    /**
     * Bid Placement with Redis Distributed Lock and Kafka Event Emission
     */
    public com.bidmesh.dto.BidResponse placeBid(Long auctionId, BidRequest bidRequest) {
        String lockKey = AUCTION_LOCK_KEY + auctionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                log.info("Lock acquired for auction {}", auctionId);

                // Fetch current state (Prefer Cache for speed during high load)
                Auction auction = getAuctionDetails(auctionId);

                // Validation
                if (auction.getStatus() != AuctionStatus.ACTIVE) {
                    throw new RuntimeException("Auction is not active");
                }
                if (bidRequest.getAmount().compareTo(auction.getCurrentPrice()) <= 0) {
                    throw new RuntimeException("Bid amount must be higher than current price: " + auction.getCurrentPrice());
                }

                // Prepare Event
                com.bidmesh.event.BidPlacedEvent event = com.bidmesh.event.BidPlacedEvent.builder()
                        .auctionId(auctionId)
                        .bidderId(bidRequest.getBidderId())
                        .amount(bidRequest.getAmount())
                        .bidTime(LocalDateTime.now())
                        .build();

                // 1. Update Cache Immediately (so next validator sees the new price)
                auction.setCurrentPrice(bidRequest.getAmount());
                redisTemplate.opsForValue().set(AUCTION_CACHE_KEY + auctionId, auction, 10, TimeUnit.MINUTES);

                // 2. Publish to Kafka (Partitioned by auctionId to ensure ordering)
                kafkaTemplate.send(AUCTION_BIDS_TOPIC, String.valueOf(auctionId), event);

                log.info("Bid event published for auction {}", auctionId);

                return com.bidmesh.dto.BidResponse.builder()
                        .amount(bidRequest.getAmount())
                        .bidTime(event.getBidTime())
                        .auctionId(auctionId)
                        .build();
            } else {
                throw new RuntimeException("Could not acquire lock for auction " + auctionId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
