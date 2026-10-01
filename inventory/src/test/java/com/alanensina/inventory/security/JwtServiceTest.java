package com.alanensina.inventory.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void validatesTokenWithSubjectAndRequiredClaims() {
        String token = token("customer", true, true, SECRET);

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void rejectsTokenWithBlankSubject() {
        String token = token(" ", true, true, SECRET);

        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void rejectsTokenMissingUsernameOrEmailClaim() {
        assertFalse(jwtService.validateToken(token("customer", false, true, SECRET)));
        assertFalse(jwtService.validateToken(token("customer", true, false, SECRET)));
    }

    @Test
    void rejectsTokenWithNullUsernameOrEmailClaim() {
        assertFalse(jwtService.validateToken(tokenWithNullUsername()));
        assertFalse(jwtService.validateToken(tokenWithNullEmail()));
    }

    @Test
    void rejectsTokenWithoutSubject() {
        String token = JWT.create()
                .withClaim("username", "user")
                .withClaim("email", "user@example.com")
                .sign(Algorithm.HMAC256(SECRET));

        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void rejectsTokenSignedByAnotherSecret() {
        assertFalse(jwtService.validateToken(token("customer", true, true, "other-secret")));
    }

    @Test
    void rejectsMalformedToken() {
        assertFalse(jwtService.validateToken("not-a-jwt"));
    }

    private String token(String subject, boolean username, boolean email, String secret) {
        var builder = JWT.create().withSubject(subject);
        if (username) {
            builder.withClaim("username", "user");
        }
        if (email) {
            builder.withClaim("email", "user@example.com");
        }
        return builder.sign(Algorithm.HMAC256(secret));
    }

    private String tokenWithNullUsername() {
        return JWT.create()
                .withSubject("customer")
                .withNullClaim("username")
                .withClaim("email", "user@example.com")
                .sign(Algorithm.HMAC256(SECRET));
    }

    private String tokenWithNullEmail() {
        return JWT.create()
                .withSubject("customer")
                .withClaim("username", "user")
                .withNullClaim("email")
                .sign(Algorithm.HMAC256(SECRET));
    }
}
