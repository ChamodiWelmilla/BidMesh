package com.bidmesh.bidding.consumer;

import com.bidmesh.bidding.messaging.BidPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BidConsumerTest {
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final BidConsumer consumer = new BidConsumer(objectMapper, messagingTemplate, redisTemplate);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void broadcastsABidOnlyOnceWhenKafkaRedeliversIt() throws Exception {
        String payload = "{\"bidId\":42}";
        BidPlacedEvent event = BidPlacedEvent.builder()
                .bidId(42L)
                .auctionId(7L)
                .bidderId(3L)
                .bidderUsername("bidder")
                .amount(new BigDecimal("125.00"))
                .bidTime(LocalDateTime.now())
                .bidCount(1)
                .build();
        when(objectMapper.readValue(payload, BidPlacedEvent.class)).thenReturn(event);
        when(valueOperations.setIfAbsent(eq("bid-notification:processed:42"), eq("1"), any()))
                .thenReturn(true, false);

        consumer.broadcastBid(payload);
        consumer.broadcastBid(payload);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/auction/7"), any(Object.class));
    }

    @Test
    void failsIfEventHasNoBidId() throws Exception {
        String payload = "{}";
        when(objectMapper.readValue(payload, BidPlacedEvent.class)).thenReturn(new BidPlacedEvent());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> consumer.broadcastBid(payload));

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void releasesDeduplicationKeyIfBroadcastFails() throws Exception {
        String payload = "{\"bidId\":42}";
        BidPlacedEvent event = BidPlacedEvent.builder()
                .bidId(42L)
                .auctionId(7L)
                .amount(new BigDecimal("125.00"))
                .build();
        when(objectMapper.readValue(payload, BidPlacedEvent.class)).thenReturn(event);
        when(valueOperations.setIfAbsent(eq("bid-notification:processed:42"), eq("1"), any()))
                .thenReturn(true);
        doThrow(new IllegalStateException("websocket send failed"))
                .when(messagingTemplate).convertAndSend(eq("/topic/auction/7"), any(Object.class));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> consumer.broadcastBid(payload));

        verify(redisTemplate).delete("bid-notification:processed:42");
    }
}
