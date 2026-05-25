package com.bidmesh.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private String bidderUsername;
    private Long auctionId;
}
