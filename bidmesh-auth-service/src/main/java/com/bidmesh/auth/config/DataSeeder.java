package com.bidmesh.auth.config;

import com.bidmesh.auth.repository.UserCredentialRepository;
import com.bidmesh.auth.repository.UserRepository;
import com.bidmesh.model.Role;
import com.bidmesh.model.User;
import com.bidmesh.model.UserCredential;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial users in Auth Service...");

            // Admin
            User adminProfile = User.builder().username("admin").build();
            userRepository.save(adminProfile);

            UserCredential adminAuth = UserCredential.builder()
                    .email("admin@bidmesh.com")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.ADMIN)
                    .user(adminProfile)
                    .build();
            userCredentialRepository.save(adminAuth);

            // User1
            User regularUserProfile = User.builder().username("user1").build();
            userRepository.save(regularUserProfile);

            UserCredential userAuth = UserCredential.builder()
                    .email("user1@bidmesh.com")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.USER)
                    .user(regularUserProfile)
                    .build();
            userCredentialRepository.save(userAuth);

            log.info("User seeding complete.");
        }
    }
}
