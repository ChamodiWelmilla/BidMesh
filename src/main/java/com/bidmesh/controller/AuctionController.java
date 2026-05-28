package com.bidmesh.controller;

import com.bidmesh.dto.BidRequest;
import com.bidmesh.model.Auction;
import com.bidmesh.model.Bid;
import com.bidmesh.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping("/{id}")
    public ResponseEntity<Auction> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionDetails(id));
    }

    @GetMapping("/all")
    public ResponseEntity<java.util.List<Auction>> getAllAuctions() {
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @PostMapping
    public ResponseEntity<Auction> createAuction(@RequestBody Auction auction, 
                                               @RequestParam Long creatorId, 
                                               @RequestParam Long itemId) {
        return ResponseEntity.ok(auctionService.createAuction(auction, creatorId, itemId));
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<com.bidmesh.dto.BidResponse> placeBid(@PathVariable Long id, @RequestBody BidRequest bidRequest) {
        return ResponseEntity.ok(auctionService.placeBid(id, bidRequest));
    }
}
