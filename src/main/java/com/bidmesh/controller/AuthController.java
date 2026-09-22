package com.bidmesh.controller;

import com.bidmesh.dto.AuthRequest;
import com.bidmesh.dto.AuthResponse;
import com.bidmesh.dto.RegisterRequest;
import com.bidmesh.model.Role;
import com.bidmesh.model.User;
import com.bidmesh.model.UserCredential;
import com.bidmesh.repository.UserCredentialRepository;
import com.bidmesh.repository.UserRepository;
import com.bidmesh.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .build();
        User savedUser = userRepository.save(user);

        UserCredential credential = UserCredential.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Default role
                .user(savedUser)
                .build();
        userCredentialRepository.save(credential);

        String jwtToken = jwtUtil.generateToken(credential);
        return ResponseEntity.ok(AuthResponse.builder().token(jwtToken).build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        UserCredential credential = userCredentialRepository.findByEmail(request.getEmail())
                .orElseThrow();
                
        String jwtToken = jwtUtil.generateToken(credential);
        return ResponseEntity.ok(AuthResponse.builder().token(jwtToken).build());
    }
}
