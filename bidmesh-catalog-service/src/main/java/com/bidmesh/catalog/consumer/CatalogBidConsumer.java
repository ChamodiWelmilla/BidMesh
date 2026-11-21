package com.bidmesh.catalog.consumer;

import com.bidmesh.catalog.repository.AuctionRepository;
import com.bidmesh.event.BidPlacedEvent;
import com.bidmesh.model.Auction;
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

    @KafkaListener(topics = "auction-bids", groupId = "bidmesh-catalog-group")
    @Transactional
    public void handleBidPlacedEvent(BidPlacedEvent event) {
        log.info("Catalog Service updating auction {} with new price {}", event.getAuctionId(), event.getAmount());
        Optional<Auction> optionalAuction = auctionRepository.findById(event.getAuctionId());
        if (optionalAuction.isPresent()) {
            Auction auction = optionalAuction.get();
            auction.setCurrentPrice(event.getAmount());
            auctionRepository.save(auction);
            redisTemplate.delete("auction:" + event.getAuctionId());
        }
    }
}
