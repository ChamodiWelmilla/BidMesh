package com.bidmesh.auth.repository;

import com.bidmesh.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId);
}
