package com.bidmesh.catalog.controller;

import com.bidmesh.catalog.service.AuctionCatalogService;
import com.bidmesh.catalog.model.Auction;
import com.bidmesh.catalog.security.CatalogPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionCatalogController {

    private final AuctionCatalogService auctionService;

    @GetMapping("/{id}")
    public ResponseEntity<Auction> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionDetails(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Auction>> getAllAuctions() {
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @PostMapping
    public ResponseEntity<Auction> createAuction(@RequestBody Auction auction,
                                                 @RequestParam Long itemId,
                                                 @AuthenticationPrincipal CatalogPrincipal principal) {
        return ResponseEntity.ok(auctionService.createAuction(
                auction, principal.userId(), principal.username(), itemId));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<Auction> endAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.endAuction(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAuction(@PathVariable Long id) {
        try {
            auctionService.deleteAuction(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}
