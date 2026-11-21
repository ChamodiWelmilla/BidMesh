package com.bidmesh.catalog.service;

import com.bidmesh.catalog.repository.AuctionRepository;
import com.bidmesh.catalog.repository.ItemRepository;
import com.bidmesh.catalog.repository.UserRepository;
import com.bidmesh.model.Auction;
import com.bidmesh.model.AuctionStatus;
import com.bidmesh.model.Item;
import com.bidmesh.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionCatalogService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final S3Service s3Service;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String AUCTION_CACHE_KEY = "auction:";

    @Transactional
    public Auction createAuction(Auction auction, Long creatorId, Long itemId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        auction.setCreator(creator);
        auction.setItem(item);
        auction.setCurrentPrice(auction.getStartPrice());
        auction.setStatus(AuctionStatus.ACTIVE);
        
        return auctionRepository.save(auction);
    }

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

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    @Transactional
    public Auction endAuction(Long id) {
        Auction auction = auctionRepository.findById(id).orElseThrow(() -> new RuntimeException("Auction not found"));
        auction.setStatus(AuctionStatus.COMPLETED);
        auction.setEndTime(LocalDateTime.now());
        auction = auctionRepository.save(auction);
        redisTemplate.delete(AUCTION_CACHE_KEY + id);
        return auction;
    }

    @Transactional
    public void deleteAuction(Long id) {
        Auction auction = auctionRepository.findById(id).orElseThrow(() -> new RuntimeException("Auction not found"));
        if (auction.getBids() != null && !auction.getBids().isEmpty()) {
            throw new RuntimeException("Cannot delete an auction that has bids. End the auction instead.");
        }
        
        if (auction.getItem() != null && auction.getItem().getImageUrl() != null) {
            s3Service.deleteImageFromS3(auction.getItem().getImageUrl());
        }
        
        auctionRepository.delete(auction);
        redisTemplate.delete(AUCTION_CACHE_KEY + id);
    }
}
