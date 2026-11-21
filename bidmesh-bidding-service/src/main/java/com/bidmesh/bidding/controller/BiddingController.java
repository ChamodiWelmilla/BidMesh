package com.bidmesh.bidding.controller;

import com.bidmesh.bidding.service.BiddingService;
import com.bidmesh.dto.BidRequest;
import com.bidmesh.dto.BidResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class BiddingController {

    private final BiddingService biddingService;

    @PostMapping("/{id}/bids")
    public ResponseEntity<BidResponse> placeBid(@PathVariable Long id, @RequestBody BidRequest bidRequest, Principal principal) {
        return ResponseEntity.ok(biddingService.placeBid(id, bidRequest, principal.getName()));
    }
}
