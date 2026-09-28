package com.alanensina.customer.services;

import com.alanensina.customer.models.Customer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret", 3_600_000);

    @Test
    void generateTokenShouldCreateValidTokenWithCustomerClaims() {
        Customer customer = customer();

        String token = jwtService.generateToken(customer);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        assertFalse(jwtService.validateToken("invalid-token"));
    }

    @Test
    void extractUsernameShouldReturnNullWhenUsernameClaimIsAbsent() {
        JwtService serviceWithSameSecret = new JwtService("test-secret", 3_600_000);
        String token = com.auth0.jwt.JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("test-secret"));

        assertNull(serviceWithSameSecret.extractUsername(token));
    }

    @Test
    void extractUsernameShouldReturnNullWhenCustomerUsernameIsNull() {
        Customer customer = customer();
        customer.setUsername(null);

        assertNull(jwtService.extractUsername(jwtService.generateToken(customer)));
    }

    @Test
    void extractUsernameShouldRejectInvalidToken() {
        assertThrows(Exception.class, () -> jwtService.extractUsername("invalid-token"));
    }

    private Customer customer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@email.com")
                .username("alice")
                .password("hashed-password")
                .build();
    }
}
