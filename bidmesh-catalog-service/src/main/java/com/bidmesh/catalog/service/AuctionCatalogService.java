package com.bidmesh.catalog.service;

import com.bidmesh.catalog.repository.AuctionRepository;
import com.bidmesh.catalog.repository.ItemRepository;
import com.bidmesh.catalog.model.Auction;
import com.bidmesh.catalog.model.AuctionStatus;
import com.bidmesh.catalog.model.Item;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionCatalogService {

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final S3Service s3Service;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuctionEventPublisher eventPublisher;

    private static final String AUCTION_CACHE_KEY = "auction:";

    @Transactional
    public Auction createAuction(Auction auction, Long creatorId, String creatorUsername, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        auction.setItem(item);
        auction.setCreatorId(creatorId);
        auction.setCreatorUsername(creatorUsername);
        auction.setCurrentPrice(auction.getStartPrice());
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBidCount(0);
        auction.setDeleted(false);

        Auction savedAuction = auctionRepository.saveAndFlush(auction);
        eventPublisher.publish(savedAuction, false);
        return savedAuction;
    }

    public Auction getAuctionDetails(Long id) {
        String key = AUCTION_CACHE_KEY + id;
        Object cachedData = redisTemplate.opsForValue().get(key);

        if (cachedData != null) {
            try {
                log.info("Auction {} retrieved from Redis cache", id);
                Auction cachedAuction = objectMapper.convertValue(cachedData, Auction.class);
                if (!cachedAuction.isDeleted()) {
                    return cachedAuction;
                }
            } catch (Exception e) {
                log.warn("Failed to convert cached data for auction {}, falling back to DB", id, e);
            }
        }

        log.info("Auction {} not in cache, fetching from PostgreSQL", id);
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        if (auction.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found");
        }

        redisTemplate.opsForValue().set(key, auction, 10, TimeUnit.MINUTES);
        return auction;
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAllByDeletedFalse();
    }

    @Transactional
    public Auction endAuction(Long id) {
        Auction auction = auctionRepository.findById(id).orElseThrow(() -> new RuntimeException("Auction not found"));
        if (auction.isDeleted()) {
            throw new RuntimeException("Auction not found");
        }
        auction.setStatus(AuctionStatus.COMPLETED);
        auction.setEndTime(LocalDateTime.now());
        auction = auctionRepository.saveAndFlush(auction);
        eventPublisher.publish(auction, false);
        redisTemplate.delete(AUCTION_CACHE_KEY + id);
        return auction;
    }

    @Transactional
    public void deleteAuction(Long id) {
        Auction auction = auctionRepository.findById(id).orElseThrow(() -> new RuntimeException("Auction not found"));
        if (auction.isDeleted()) {
            throw new RuntimeException("Auction not found");
        }
        if (auction.getBidCount() > 0) {
            throw new RuntimeException("Cannot delete an auction that has bids. End the auction instead.");
        }
        
        if (auction.getItem() != null && auction.getItem().getImageUrl() != null) {
            s3Service.deleteImageFromS3(auction.getItem().getImageUrl());
        }
        
        auction.setDeleted(true);
        auction.setStatus(AuctionStatus.CANCELLED);
        auction = auctionRepository.saveAndFlush(auction);
        eventPublisher.publish(auction, true);
        redisTemplate.delete(AUCTION_CACHE_KEY + id);
    }
}
