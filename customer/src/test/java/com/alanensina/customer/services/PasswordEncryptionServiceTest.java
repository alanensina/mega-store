package com.alanensina.customer.services;

import com.alanensina.customer.exceptions.PasswordHashingFailedException;
import com.alanensina.customer.exceptions.PasswordMatchingFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordEncryptionServiceTest {

    private final PasswordEncryptionService service = new PasswordEncryptionService();

    @Test
    void encryptPasswordShouldReturnABcryptHash() {
        String hash = service.encryptPassword("Password123");

        assertNotNull(hash);
        assertNotEquals("Password123", hash);
        assertTrue(service.matchPassword("Password123", hash));
    }

    @Test
    void encryptPasswordShouldWrapEncodingErrors() {
        try (var _ =
                     mockConstruction(BCryptPasswordEncoder.class,
                             (mock, context) -> when(mock.encode("Password123"))
                                     .thenThrow(new IllegalStateException("encoder error")))) {
            PasswordEncryptionService serviceWithMockedEncoder = new PasswordEncryptionService();

            PasswordHashingFailedException exception = assertThrows(
                    PasswordHashingFailedException.class,
                    () -> serviceWithMockedEncoder.encryptPassword("Password123"));

            assertEquals("Password hashing failed due to an internal error", exception.getMessage());
        }
    }

    @Test
    void matchPasswordShouldReturnFalseForDifferentPassword() {
        String hash = service.encryptPassword("Password123");

        assertFalse(service.matchPassword("Different123", hash));
    }

    @Test
    void matchPasswordShouldWrapMatchingErrors() {
        try (var _ =
                     mockConstruction(BCryptPasswordEncoder.class,
                             (mock, context) -> when(mock.matches("Password123", "hash"))
                                     .thenThrow(new IllegalStateException("matching error")))) {
            PasswordEncryptionService serviceWithMockedEncoder = new PasswordEncryptionService();

            PasswordMatchingFailedException exception = assertThrows(
                    PasswordMatchingFailedException.class,
                    () -> serviceWithMockedEncoder.matchPassword("Password123", "hash"));

            assertTrue(exception.getMessage().startsWith("Password matching failed, error:"));
        }
    }
}
