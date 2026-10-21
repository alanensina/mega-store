package com.alanensina.order.security;

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

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret)).build();
    }

    public boolean validateToken(String token) {
        try {
            var jwt = verifier.verify(token);
            var username = jwt.getClaim("username");
            var email = jwt.getClaim("email");
            return jwt.getSubject() != null
                    && !jwt.getSubject().isBlank()
                    && !username.isMissing()
                    && !username.isNull()
                    && !email.isMissing()
                    && !email.isNull();
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            return false;
        }
    }
}
