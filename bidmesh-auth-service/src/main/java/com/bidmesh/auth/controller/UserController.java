package com.bidmesh.auth.controller;

import com.bidmesh.auth.repository.UserCredentialRepository;
import com.bidmesh.auth.repository.UserRepository;
import com.bidmesh.auth.dto.AuthResponse;
import com.bidmesh.auth.dto.UserUpdateRequest;
import com.bidmesh.auth.model.UserCredential;
import com.bidmesh.auth.security.JwtUtil;
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
}
