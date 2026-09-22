package com.bidmesh.config;

import com.bidmesh.model.Auction;
import com.bidmesh.model.AuctionStatus;
import com.bidmesh.model.User;
import com.bidmesh.model.UserCredential;
import com.bidmesh.repository.AuctionRepository;
import com.bidmesh.repository.UserRepository;
import com.bidmesh.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final AuctionRepository auctionRepository;
    private final com.bidmesh.repository.ItemRepository itemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Seeding initial data...");

            // --- ADMIN USER ---
            User adminProfile = User.builder()
                    .username("admin")
                    .build();
            userRepository.save(adminProfile);

            UserCredential adminAuth = UserCredential.builder()
                    .email("admin@bidmesh.com")
                    .password(passwordEncoder.encode("password"))
                    .role(com.bidmesh.model.Role.ADMIN)
                    .user(adminProfile)
                    .build();
            userCredentialRepository.save(adminAuth);

            // --- REGULAR USER ---
            User regularUserProfile = User.builder()
                    .username("user1")
                    .build();
            userRepository.save(regularUserProfile);

            UserCredential userAuth = UserCredential.builder()
                    .email("user1@bidmesh.com")
                    .password(passwordEncoder.encode("password"))
                    .role(com.bidmesh.model.Role.USER)
                    .user(regularUserProfile)
                    .build();
            userCredentialRepository.save(userAuth);

            // --- SEED ITEMS ---
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

            // --- SEED AUCTIONS ---
            Auction watch = Auction.builder()
                    .item(watchItem)
                    .startPrice(new BigDecimal("5000.00"))
                    .currentPrice(new BigDecimal("5000.00"))
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(7))
                    .status(AuctionStatus.ACTIVE)
                    .creator(adminProfile)
                    .build();

            Auction guitar = Auction.builder()
                    .item(guitarItem)
                    .startPrice(new BigDecimal("25000.00"))
                    .currentPrice(new BigDecimal("25000.00"))
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(3))
                    .status(AuctionStatus.ACTIVE)
                    .creator(adminProfile)
                    .build();

            auctionRepository.saveAll(List.of(watch, guitar));
            log.info("Data seeding complete.");
        }
    }
}
