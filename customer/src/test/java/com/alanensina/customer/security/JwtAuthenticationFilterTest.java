package com.alanensina.customer.security;

import com.alanensina.customer.models.Customer;
import com.alanensina.customer.services.CustomerService;
import com.alanensina.customer.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomerService customerService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
        doFilter(null);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, customerService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic credentials");

        doFilter(request.getHeader(HttpHeaders.AUTHORIZATION));

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, customerService);
    }

    @Test
    void shouldContinueWhenTokenIsInvalid() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid");
        when(jwtService.validateToken("invalid")).thenReturn(false);

        doFilter(request.getHeader(HttpHeaders.AUTHORIZATION));

        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken("invalid");
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void shouldContinueWhenTokenHasNoUsername() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token");
        when(jwtService.validateToken("token")).thenReturn(true);
        when(jwtService.extractUsername("token")).thenReturn(null);

        doFilter(request.getHeader(HttpHeaders.AUTHORIZATION));

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(customerService);
    }

    @Test
    void shouldAuthenticateCustomerForValidToken() throws Exception {
        Customer customer = Customer.builder().username("alice").build();
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token");
        when(jwtService.validateToken("token")).thenReturn(true);
        when(jwtService.extractUsername("token")).thenReturn("alice");
        when(customerService.getCustomerByUsername("alice")).thenReturn(customer);

        doFilter(request.getHeader(HttpHeaders.AUTHORIZATION));

        assertEquals(customer, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    private void doFilter(String ignoredHeader) throws ServletException, IOException {
        if (ignoredHeader != null) {
            when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(ignoredHeader);
        }
        new ExposedFilter(jwtService, customerService).run(request, response, filterChain);
    }

    private static class ExposedFilter extends JwtAuthenticationFilter {
        private ExposedFilter(JwtService jwtService, CustomerService customerService) {
            super(jwtService, customerService);
        }

        private void run(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            doFilterInternal(request, response, chain);
        }
    }
}
