package com.bidmesh.catalog.repository;

import com.bidmesh.catalog.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findAllByDeletedFalse();
}
