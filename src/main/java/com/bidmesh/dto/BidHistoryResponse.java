package com.bidmesh.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidHistoryResponse {
    private Long bidId;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private Long auctionId;
    private String itemName;
}
