package com.alanensina.customer.services;

import com.alanensina.customer.exceptions.PasswordHashingFailedException;
import com.alanensina.customer.exceptions.PasswordMatchingFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PasswordEncryptionService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public String encryptPassword(String password) {
        try {
            return passwordEncoder.encode(password);
        } catch (Exception e) {
            log.error("Error hashing password: ", e);
            throw new PasswordHashingFailedException();
        }
    }

    public boolean matchPassword(String plainPassword, String hashedPassword) {
        try {
            return passwordEncoder.matches(plainPassword, hashedPassword);
        } catch (Exception e) {
            throw new PasswordMatchingFailedException(e.getMessage());
        }
    }
}
