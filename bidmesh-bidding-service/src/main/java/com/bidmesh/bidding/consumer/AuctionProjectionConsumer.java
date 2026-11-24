package com.bidmesh.bidding.consumer;

import com.bidmesh.bidding.messaging.AuctionChangedEvent;
import com.bidmesh.bidding.model.AuctionProjection;
import com.bidmesh.bidding.model.AuctionStatus;
import com.bidmesh.bidding.repository.AuctionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionProjectionConsumer {
    private final AuctionRepository auctionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "auction-lifecycle", groupId = "bidmesh-bidding-projections")
    @Transactional
    public void updateProjection(String payload) throws JsonProcessingException {
        AuctionChangedEvent event = objectMapper.readValue(payload, AuctionChangedEvent.class);
        AuctionProjection auction = auctionRepository.findById(event.getAuctionId())
                .orElseGet(() -> AuctionProjection.builder()
                        .id(event.getAuctionId())
                        .startPrice(event.getStartPrice())
                        .currentPrice(event.getCurrentPrice())
                        .status(AuctionStatus.valueOf(event.getStatus()))
                        .build());

        if (event.getCatalogVersion() <= auction.getCatalogVersion()) {
            return;
        }

        auction.setCatalogVersion(event.getCatalogVersion());
        auction.setItemId(event.getItemId());
        auction.setItemName(event.getItemName());
        auction.setItemDescription(event.getItemDescription());
        auction.setItemImageUrl(event.getItemImageUrl());
        auction.setCreatorId(event.getCreatorId());
        auction.setCreatorUsername(event.getCreatorUsername());
        auction.setStartPrice(event.getStartPrice());
        if (auction.getCurrentPrice() == null
                || event.getCurrentPrice().compareTo(auction.getCurrentPrice()) > 0) {
            auction.setCurrentPrice(event.getCurrentPrice());
        }
        auction.setStartTime(event.getStartTime());
        auction.setEndTime(event.getEndTime());
        auction.setStatus(AuctionStatus.valueOf(event.getStatus()));
        auction.setBidCount(Math.max(auction.getBidCount(), event.getBidCount()));
        auction.setDeleted(event.isDeleted());
        auctionRepository.save(auction);
        redisTemplate.delete("auction:" + event.getAuctionId());
        log.info("Applied catalog version {} for auction {}", event.getCatalogVersion(), event.getAuctionId());
    }
}
