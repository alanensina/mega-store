package com.alanensina.inventory.security;

import com.alanensina.inventory.exceptions.SecurityFilterChainFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityConfigTest {

    @Test
    void createsJwtFilterAndStatelessSecurityChain() throws Exception {
        SecurityConfig config = new SecurityConfig(new JwtService("test-secret"));

        assertInstanceOf(JwtAuthenticationFilter.class, config.jwtAuthenticationFilter());

        HttpSecurity http = mock(HttpSecurity.class);
        configureDsl(http);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        assertSame(chain, config.securityFilterChain(http));
        verify(http).addFilterBefore(any(JwtAuthenticationFilter.class), any());
    }

    @Test
    void wrapsSecurityChainConfigurationFailures() throws Exception {
        SecurityConfig config = new SecurityConfig(new JwtService("test-secret"));
        HttpSecurity http = mock(HttpSecurity.class);
        configureDsl(http);
        when(http.build()).thenThrow(new IllegalStateException("security setup failed"));

        SecurityFilterChainFailedException exception = assertThrows(SecurityFilterChainFailedException.class,
                () -> config.securityFilterChain(http));

        assertTrue(exception.getMessage().contains("security setup failed"));
    }

    private void configureDsl(HttpSecurity http) throws Exception {
        var csrf = mock(CsrfConfigurer.class);
        when(csrf.disable()).thenReturn(http);
        when(http.csrf(any())).thenAnswer(invocation -> {
            Customizer<CsrfConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            customizer.customize(csrf);
            return http;
        });

        var session = mock(SessionManagementConfigurer.class);
        when(http.sessionManagement(any())).thenAnswer(invocation -> {
            Customizer<SessionManagementConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            customizer.customize(session);
            return http;
        });

        var registry = mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);
        var authorized = mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);
        when(registry.requestMatchers(any(String[].class))).thenReturn(authorized);
        when(authorized.permitAll()).thenReturn(registry);
        when(registry.anyRequest()).thenReturn(authorized);
        when(authorized.authenticated()).thenReturn(registry);
        when(http.authorizeHttpRequests(any())).thenAnswer(invocation -> {
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
                    customizer = invocation.getArgument(0);
            customizer.customize(registry);
            return http;
        });
        when(http.addFilterBefore(any(), any())).thenReturn(http);
    }
}
