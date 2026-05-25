package com.bidmesh.consumer;

import com.bidmesh.event.BidPlacedEvent;
import com.bidmesh.model.Auction;
import com.bidmesh.model.Bid;
import com.bidmesh.model.User;
import com.bidmesh.repository.AuctionRepository;
import com.bidmesh.repository.BidRepository;
import com.bidmesh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidConsumer {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "auction-bids", groupId = "bidmesh-group")
    @Transactional
    public void handleBidPlacedEvent(BidPlacedEvent event) {
        log.info("Consuming bid event for auction {}: Amount {}", event.getAuctionId(), event.getAmount());

        try {
            Auction auction = auctionRepository.findById(event.getAuctionId())
                    .orElseThrow(() -> new RuntimeException("Auction not found"));

            User bidder = userRepository.findById(event.getBidderId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Double check validation
            if (event.getAmount().compareTo(auction.getCurrentPrice()) <= 0) {
                log.warn("Delayed event for lower bid detected. Skipping persistence.");
                return;
            }

            // Persistence
            Bid bid = Bid.builder()
                    .amount(event.getAmount())
                    .bidTime(event.getBidTime())
                    .auction(auction)
                    .bidder(bidder)
                    .build();

            auction.setCurrentPrice(event.getAmount());
            
            bidRepository.save(bid);
            auctionRepository.save(auction);

            // BROADCAST via WebSocket
            com.bidmesh.dto.BidNotification notification = com.bidmesh.dto.BidNotification.builder()
                    .auctionId(event.getAuctionId())
                    .currentPrice(event.getAmount())
                    .lastBidder(bidder.getUsername())
                    .bidTime(event.getBidTime())
                    .build();

            messagingTemplate.convertAndSend("/topic/auction/" + event.getAuctionId(), notification);

            log.info("Successfully persisted and broadcasted bid for auction {}", event.getAuctionId());
        } catch (Exception e) {
            log.error("Error processing bid event: {}", e.getMessage());
        }
    }
}
