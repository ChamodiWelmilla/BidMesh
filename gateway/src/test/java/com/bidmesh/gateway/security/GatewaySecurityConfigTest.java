package com.bidmesh.gateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewaySecurityConfigTest {
    private static final String SHARED_SECRET = "SecretKeyForBidMeshAuthentication2026";

    private final GatewaySecurityConfig config = new GatewaySecurityConfig();

    @Test
    void decodesTokenSignedWithSharedSecret() throws Exception {
        ReactiveJwtDecoder decoder = config.jwtDecoder(SHARED_SECRET);
        String token = signedToken(SHARED_SECRET);

        assertEquals("bidder@example.com", decoder.decode(token).block().getSubject());
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() throws Exception {
        ReactiveJwtDecoder decoder = config.jwtDecoder(SHARED_SECRET);
        String token = signedToken("AnotherSecretThatIsLongEnoughForHS256");

        assertThrows(JwtException.class, () -> decoder.decode(token).block());
    }

    private static String signedToken(String secret) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("bidder@example.com")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
