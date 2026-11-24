package com.bidmesh.auth.security;

import com.bidmesh.auth.model.UserCredential;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey signingKey;

    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserCredential credential) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(credential.getEmail())
                .claim("userId", credential.getUser().getId())
                .claim("displayName", credential.getUser().getUsername())
                .claim("role", credential.getAuthorities().iterator().next().getAuthority())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 1000L * 60 * 60 * 10))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = parseToken(token);
        return userDetails.getUsername().equals(claims.getSubject())
                && claims.getExpiration().after(new Date());
    }
}
