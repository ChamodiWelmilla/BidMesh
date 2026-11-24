package com.bidmesh.bidding.consumer;

import com.bidmesh.bidding.dto.BidNotification;
import com.bidmesh.bidding.messaging.BidPlacedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidConsumer {
    private static final String PROCESSED_BID_KEY_PREFIX = "bid-notification:processed:";
    private static final Duration DEDUPLICATION_TTL = Duration.ofDays(8);

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "auction-bids", groupId = "bidmesh-bidding-websocket")
    public void broadcastBid(String payload) throws JsonProcessingException {
        BidPlacedEvent event = objectMapper.readValue(payload, BidPlacedEvent.class);
        if (event.getBidId() == null) {
            throw new IllegalArgumentException("Bid event is missing its bid ID");
        }

        String processedKey = PROCESSED_BID_KEY_PREFIX + event.getBidId();
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(processedKey, "1", DEDUPLICATION_TTL);
        if (claimed == null) {
            throw new IllegalStateException("Could not claim bid notification idempotency key");
        }
        if (!claimed) {
            log.debug("Skipping duplicate bid notification for bid {}", event.getBidId());
            return;
        }

        BidNotification notification = BidNotification.builder()
                .auctionId(event.getAuctionId())
                .currentPrice(event.getAmount())
                .lastBidder(event.getBidderUsername())
                .bidTime(event.getBidTime())
                .bidCount(event.getBidCount())
                .build();

        try {
            messagingTemplate.convertAndSend("/topic/auction/" + event.getAuctionId(), notification);
        } catch (RuntimeException e) {
            redisTemplate.delete(processedKey);
            throw e;
        }
        log.info("Broadcast bid for auction {} at {}", event.getAuctionId(), event.getAmount());
    }
}
