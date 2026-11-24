package com.bidmesh.catalog.service;

import com.bidmesh.catalog.messaging.AuctionChangedEvent;
import com.bidmesh.catalog.model.Auction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class AuctionEventPublisher {
    private static final String TOPIC = "auction-lifecycle";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Auction auction, boolean deleted) {
        AuctionChangedEvent event = AuctionChangedEvent.builder()
                .auctionId(auction.getId())
                .catalogVersion(auction.getVersion() == null ? 0 : auction.getVersion())
                .itemId(auction.getItem().getId())
                .itemName(auction.getItem().getName())
                .itemDescription(auction.getItem().getDescription())
                .itemImageUrl(auction.getItem().getImageUrl())
                .creatorId(auction.getCreatorId())
                .creatorUsername(auction.getCreatorUsername())
                .startPrice(auction.getStartPrice())
                .currentPrice(auction.getCurrentPrice())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus().name())
                .bidCount(auction.getBidCount())
                .deleted(deleted)
                .build();
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, auction.getId().toString(), payload)
                    .get(10, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize auction lifecycle event", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing auction lifecycle event", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish auction lifecycle event", e);
        }
    }
}
