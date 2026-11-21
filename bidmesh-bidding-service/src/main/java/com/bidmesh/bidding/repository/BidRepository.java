package com.bidmesh.bidding.repository;

import com.bidmesh.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
    boolean existsByKafkaMessageId(String kafkaMessageId);
}
