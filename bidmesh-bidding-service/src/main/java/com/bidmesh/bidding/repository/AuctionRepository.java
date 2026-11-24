package com.bidmesh.bidding.repository;

import com.bidmesh.bidding.model.AuctionProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<AuctionProjection, Long> {
}
