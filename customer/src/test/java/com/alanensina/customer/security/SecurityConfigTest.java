package com.alanensina.customer.security;

import com.alanensina.customer.exceptions.SecurityFilterChainFailedException;
import com.alanensina.customer.services.CustomerService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
@Import({SecurityConfig.class, SecurityConfigTest.Dependencies.class})
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldCreateSecurityFilterChain() {
        assertNotNull(securityFilterChain);
    }

    @Test
    void shouldApplyCsrfExceptionsOnlyToConfiguredApiRequests() throws Exception {
        Filter csrfFilter = securityFilterChain.getFilters().stream()
                .filter(CsrfFilter.class::isInstance)
                .findFirst()
                .orElseThrow();

        applyCsrf(csrfFilter, null, "GET");
        applyCsrf(csrfFilter, "/api/auth", "POST");
        applyCsrf(csrfFilter, "/api/auth/login", "POST");
        applyCsrf(csrfFilter, "/api/customer", "POST");
        applyCsrf(csrfFilter, "/api/customer", "PUT");
        applyCsrf(csrfFilter, "/api/customer", "DELETE");
        applyCsrf(csrfFilter, "/api/customer", "GET");
        applyCsrf(csrfFilter, "/api/customer", "PATCH");
        applyCsrf(csrfFilter, "/other", "GET");
        applyCsrf(csrfFilter, "/other", "POST");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn(null);
        when(request.getContextPath()).thenReturn("");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(80);
        when(request.getMethod()).thenReturn("POST");

        MockHttpServletResponse response = new MockHttpServletResponse();

        csrfFilter.doFilter(request, response, (req, resp) -> {
        });


        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    void shouldWrapSecurityConfigurationFailures() {
        SecurityConfig config = new SecurityConfig(new JwtService("test-secret", 3_600_000),
                mock(CustomerService.class));
        HttpSecurity http =
                mock(HttpSecurity.class,
                        RETURNS_DEEP_STUBS);
        when(http.build()).thenThrow(new RuntimeException("configuration failure"));

        var exception = Assertions.assertThrows(
                SecurityFilterChainFailedException.class,
                () -> config.securityFilterChain(http));

        Assertions.assertTrue(exception.getMessage().contains("configuration failure"));
    }

    private void applyCsrf(Filter filter, String path, String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        if (path != null) {
            request.setServletPath(path);
        }
        filter.doFilter(request, new MockHttpServletResponse(), (FilterChain) (req, response) -> {
        });
    }

    static class Dependencies {
        @Bean
        JwtService jwtService() {
            return new JwtService("test-secret", 3_600_000);
        }

        @Bean
        CustomerService customerService() {
            return mock(CustomerService.class);
        }
    }
}
