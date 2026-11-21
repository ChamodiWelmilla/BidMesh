package com.bidmesh.dto;

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
public class BidNotification {
    private Long auctionId;
    private BigDecimal currentPrice;
    private String lastBidder;
    private LocalDateTime bidTime;
}
