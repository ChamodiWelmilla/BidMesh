package com.bidmesh.config;

import com.bidmesh.model.Auction;
import com.bidmesh.model.AuctionStatus;
import com.bidmesh.model.User;
import com.bidmesh.repository.AuctionRepository;
import com.bidmesh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Seeding initial data...");

            User admin = User.builder()
                    .username("admin")
                    .email("admin@bidmesh.com")
                    .build();
            userRepository.save(admin);

            Auction watch = Auction.builder()
                    .itemName("Vintage Rolex Submariner")
                    .description("A classic 1970s diving watch in excellent condition.")
                    .startPrice(new BigDecimal("5000.00"))
                    .currentPrice(new BigDecimal("5000.00"))
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(7))
                    .status(AuctionStatus.ACTIVE)
                    .creator(admin)
                    .build();

            Auction guitar = Auction.builder()
                    .itemName("1959 Gibson Les Paul")
                    .description("Rare sunburst finish, all original parts.")
                    .startPrice(new BigDecimal("25000.00"))
                    .currentPrice(new BigDecimal("25000.00"))
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(3))
                    .status(AuctionStatus.ACTIVE)
                    .creator(admin)
                    .build();

            auctionRepository.saveAll(List.of(watch, guitar));
            log.info("Data seeding complete.");
        }
    }
}
