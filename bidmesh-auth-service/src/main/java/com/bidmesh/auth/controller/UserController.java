package com.bidmesh.auth.controller;

import com.bidmesh.auth.repository.BidRepository;
import com.bidmesh.auth.repository.UserCredentialRepository;
import com.bidmesh.auth.repository.UserRepository;
import com.bidmesh.dto.AuthResponse;
import com.bidmesh.dto.BidHistoryResponse;
import com.bidmesh.dto.UserUpdateRequest;
import com.bidmesh.model.Bid;
import com.bidmesh.model.UserCredential;
import com.bidmesh.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class UserController {

    private final UserCredentialRepository userCredentialRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
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

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() && !request.getEmail().equals(currentEmail)) {
            if (userCredentialRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Error: Email is already in use by another account.");
            }
            credential.setEmail(request.getEmail());
        }

        userCredentialRepository.save(credential);

        String newToken = jwtUtil.generateToken(credential);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(newToken)
                .build());
    }

    @GetMapping("/me/bids")
    public ResponseEntity<List<BidHistoryResponse>> getMyBids(Authentication authentication) {
        String currentEmail = authentication.getName();
        UserCredential credential = userCredentialRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Bid> bids = bidRepository.findByBidderIdOrderByBidTimeDesc(credential.getUser().getId());

        List<BidHistoryResponse> response = bids.stream().map(bid -> 
            BidHistoryResponse.builder()
                .bidId(bid.getId())
                .amount(bid.getAmount())
                .bidTime(bid.getBidTime())
                .auctionId(bid.getAuction().getId())
                .itemName(bid.getAuction().getItem().getName())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
