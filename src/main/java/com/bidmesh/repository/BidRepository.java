package com.bidmesh.repository;

import com.bidmesh.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    /**
     * Used for idempotency to prevent duplicate bid processing.
     * 
     * @param kafkaMessageId
     * @return
     */
    boolean existsByKafkaMessageId(String kafkaMessageId);
}
