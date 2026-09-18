package com.alanensina.customer.services;

import com.alanensina.customer.models.Customer;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final Algorithm algorithm;
    private final long expirationMs;
    private final JWTVerifier verifier;

    public JwtService(@Value("${jwt.secret:changeit}") String secret,
                      @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMs = expirationMs;
        this.verifier = JWT.require(algorithm).build();
    }

    public String generateToken(Customer customer) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMs);

        return JWT.create()
                .withSubject(customer.getId().toString())
                .withClaim("username", customer.getUsername())
                .withClaim("email", customer.getEmail())
                // JWT library requires java.util.Date; conversions from java.time.Instant are intentional
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .sign(algorithm);
    }

    public boolean validateToken(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: " + e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        DecodedJWT jwt = verifier.verify(token);
        var claim = jwt.getClaim("username");
        return claim.isNull() ? null : claim.asString();
    }
}
