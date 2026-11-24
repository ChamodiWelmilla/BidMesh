package com.bidmesh.bidding.controller;

import com.bidmesh.bidding.service.BiddingService;
import com.bidmesh.bidding.dto.BidRequest;
import com.bidmesh.bidding.dto.BidResponse;
import com.bidmesh.bidding.security.BidderPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class BiddingController {

    private final BiddingService biddingService;

    @PostMapping("/{id}/bids")
    public ResponseEntity<BidResponse> placeBid(@PathVariable Long id,
                                                @RequestBody BidRequest bidRequest,
                                                @AuthenticationPrincipal BidderPrincipal bidder) {
        return ResponseEntity.ok(biddingService.placeBid(id, bidRequest, bidder));
    }
}
