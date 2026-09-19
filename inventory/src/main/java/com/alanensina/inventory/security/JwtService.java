package com.alanensina.inventory.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

    private final JWTVerifier verifier;

    public JwtService(@Value("${jwt.secret:changeit}") String secret) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret)).build();
    }

    public boolean validateToken(String token) {
        try {
            var jwt = verifier.verify(token);
            return jwt.getSubject() != null
                    && !jwt.getSubject().isBlank()
                    && !jwt.getClaim("username").isNull()
                    && !jwt.getClaim("email").isNull();
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            return false;
        }
    }
}
