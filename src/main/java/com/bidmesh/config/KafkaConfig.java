package com.bidmesh.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String AUCTION_BIDS_TOPIC = "auction-bids";

    @Bean
    public NewTopic auctionBidsTopic() {
        return TopicBuilder.name(AUCTION_BIDS_TOPIC)
                .partitions(3)
                .replicas(3)
                .build();
    }
}
