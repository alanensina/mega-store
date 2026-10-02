package com.alanensina.order.security;

import com.alanensina.order.exceptions.SecurityFilterChainFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void createsJwtFilter() {
        JwtAuthenticationFilter filter = new SecurityConfig(mock(JwtService.class)).jwtAuthenticationFilter();

        org.junit.jupiter.api.Assertions.assertNotNull(filter);
    }

    @Test
    void wrapsSecurityConfigurationFailures() {
        HttpSecurity http = mock(HttpSecurity.class);
        doThrow(new IllegalStateException("configuration failed")).when(http).csrf(any());
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtService.class));

        assertThrows(SecurityFilterChainFailedException.class,
                () -> securityConfig.securityFilterChain(http));
    }
}
