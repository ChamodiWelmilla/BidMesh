package com.bidmesh.bidding.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BidRequest {
    private BigDecimal amount;
}
