package com.bidmesh.controller;

import com.bidmesh.dto.AuthResponse;
import com.bidmesh.dto.UserUpdateRequest;
import com.bidmesh.model.UserCredential;
import com.bidmesh.repository.UserCredentialRepository;
import com.bidmesh.repository.UserRepository;
import com.bidmesh.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class UserController {

    private final UserCredentialRepository userCredentialRepository;
    private final UserRepository userRepository;
    private final com.bidmesh.repository.BidRepository bidRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody UserUpdateRequest request) {
        String currentEmail = authentication.getName();
        
        UserCredential credential = userCredentialRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            credential.getUser().setUsername(request.getUsername());
            userRepository.save(credential.getUser());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            credential.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        boolean emailChanged = false;
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() && !request.getEmail().equals(currentEmail)) {
            if (userCredentialRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Error: Email is already in use by another account.");
            }
            credential.setEmail(request.getEmail());
            emailChanged = true;
        }

        userCredentialRepository.save(credential);

        String newToken = jwtUtil.generateToken(credential);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(newToken)
                .build());
    }

    @GetMapping("/me/bids")
    public ResponseEntity<java.util.List<com.bidmesh.dto.BidHistoryResponse>> getMyBids(Authentication authentication) {
        String currentEmail = authentication.getName();
        UserCredential credential = userCredentialRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.List<com.bidmesh.model.Bid> bids = bidRepository.findByBidderIdOrderByBidTimeDesc(credential.getUser().getId());

        java.util.List<com.bidmesh.dto.BidHistoryResponse> response = bids.stream().map(bid -> 
            com.bidmesh.dto.BidHistoryResponse.builder()
                .bidId(bid.getId())
                .amount(bid.getAmount())
                .bidTime(bid.getBidTime())
                .auctionId(bid.getAuction().getId())
                .itemName(bid.getAuction().getItem().getName())
                .build()
        ).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
