package com.alanensina.order.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "coverage-test-secret";
    private final JwtService jwtService = new JwtService(SECRET);
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET);

    @Test
    void acceptsTokenWithSubjectAndRequiredClaims() {
        assertTrue(jwtService.validateToken(token("customer", true, true)));
    }

    @Test
    void rejectsTokenWithoutSubject() {
        String token = JWT.create()
                .withClaim("username", "alice")
                .withClaim("email", "alice@example.com")
                .sign(algorithm);

        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void rejectsTokenWithBlankSubject() {
        assertFalse(jwtService.validateToken(token("  ", true, true)));
    }

    @Test
    void rejectsTokenWithoutUsernameClaim() {
        assertFalse(jwtService.validateToken(token("customer", false, true)));
    }

    @Test
    void rejectsTokenWithNullUsernameClaim() {
        assertFalse(jwtService.validateToken(tokenWithNullClaim("username")));
    }

    @Test
    void rejectsTokenWithoutEmailClaim() {
        assertFalse(jwtService.validateToken(token("customer", true, false)));
    }

    @Test
    void rejectsTokenWithNullEmailClaim() {
        assertFalse(jwtService.validateToken(tokenWithNullClaim("email")));
    }

    @Test
    void rejectsMalformedToken() {
        assertFalse(jwtService.validateToken("not-a-jwt"));
    }

    private String token(String subject, boolean username, boolean email) {
        var builder = JWT.create().withSubject(subject).withExpiresAt(new Date(System.currentTimeMillis() + 60_000));
        if (username) {
            builder.withClaim("username", "alice");
        }
        if (email) {
            builder.withClaim("email", "alice@example.com");
        }
        return builder.sign(algorithm);
    }

    private String tokenWithNullClaim(String nullClaim) {
        var builder = JWT.create().withSubject("customer").withExpiresAt(new Date(System.currentTimeMillis() + 60_000));
        if ("username".equals(nullClaim)) {
            builder.withNullClaim("username").withClaim("email", "alice@example.com");
        } else {
            builder.withClaim("username", "alice").withNullClaim("email");
        }
        return builder.sign(algorithm);
    }
}
