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
public class AuctionChangedEvent {
    private Long auctionId;
    private long catalogVersion;
    private Long itemId;
    private String itemName;
    private String itemDescription;
    private String itemImageUrl;
    private Long creatorId;
    private String creatorUsername;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private long bidCount;
    private boolean deleted;
}
