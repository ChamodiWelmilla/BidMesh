package com.bidmesh.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BidRequest {
    private Long bidderId;
    private BigDecimal amount;
}
