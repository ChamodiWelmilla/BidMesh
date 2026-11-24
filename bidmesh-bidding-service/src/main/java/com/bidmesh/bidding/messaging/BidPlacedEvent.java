package com.bidmesh.bidding.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidPlacedEvent {
    private Long bidId;
    private Long auctionId;
    private Long bidderId;
    private String bidderUsername;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private long bidCount;
}
