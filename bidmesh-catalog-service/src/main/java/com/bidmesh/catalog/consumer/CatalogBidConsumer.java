package com.bidmesh.catalog.consumer;

import com.bidmesh.catalog.repository.AuctionRepository;
import com.bidmesh.catalog.messaging.BidPlacedEvent;
import com.bidmesh.catalog.model.Auction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogBidConsumer {

    private final AuctionRepository auctionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "auction-bids", groupId = "bidmesh-catalog-group")
    @Transactional
    public void handleBidPlacedEvent(String payload) throws JsonProcessingException {
        BidPlacedEvent event = objectMapper.readValue(payload, BidPlacedEvent.class);
        log.info("Catalog Service updating auction {} with new price {}", event.getAuctionId(), event.getAmount());
        Optional<Auction> optionalAuction = auctionRepository.findById(event.getAuctionId());
        if (optionalAuction.isPresent()) {
            Auction auction = optionalAuction.get();
            if (auction.isDeleted() || event.getBidCount() <= auction.getBidCount()) {
                return;
            }
            auction.setCurrentPrice(event.getAmount());
            auction.setBidCount(event.getBidCount());
            auctionRepository.save(auction);
            redisTemplate.delete("auction:" + event.getAuctionId());
        }
    }
}
