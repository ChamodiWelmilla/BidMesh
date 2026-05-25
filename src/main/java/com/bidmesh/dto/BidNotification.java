package com.bidmesh.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidNotification {
    private Long auctionId;
    private BigDecimal currentPrice;
    private String lastBidder;
    private LocalDateTime bidTime;
}
