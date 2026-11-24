package com.bidmesh.bidding.controller;

import com.bidmesh.bidding.dto.BidHistoryResponse;
import com.bidmesh.bidding.security.BidderPrincipal;
import com.bidmesh.bidding.service.BiddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidHistoryController {
    private final BiddingService biddingService;

    @GetMapping("/me")
    public List<BidHistoryResponse> getMyBids(@AuthenticationPrincipal BidderPrincipal bidder) {
        return biddingService.getBidHistory(bidder.userId());
    }
}
