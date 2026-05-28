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
    private final com.bidmesh.repository.ItemRepository itemRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Seeding initial data...");

            User admin = User.builder()
                    .username("admin")
                    .email("admin@bidmesh.com")
                    .role(com.bidmesh.model.Role.ADMIN)
                    .build();
            userRepository.save(admin);

            User regularUser = User.builder()
                    .username("user1")
                    .email("user1@bidmesh.com")
                    .role(com.bidmesh.model.Role.USER)
                    .build();
            userRepository.save(regularUser);

            com.bidmesh.model.Item watchItem = com.bidmesh.model.Item.builder()
                    .name("Vintage Rolex Submariner")
                    .description("A classic 1970s diving watch in excellent condition.")
                    .category("Luxury Watches")
                    .build();

            com.bidmesh.model.Item guitarItem = com.bidmesh.model.Item.builder()
                    .name("1959 Gibson Les Paul")
                    .description("Rare sunburst finish, all original parts.")
                    .category("Musical Instruments")
                    .build();

            itemRepository.saveAll(List.of(watchItem, guitarItem));

            Auction watch = Auction.builder()
                    .item(watchItem)
                    .startPrice(new BigDecimal("5000.00"))
                    .currentPrice(new BigDecimal("5000.00"))
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(7))
                    .status(AuctionStatus.ACTIVE)
                    .creator(admin)
                    .build();

            Auction guitar = Auction.builder()
                    .item(guitarItem)
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
