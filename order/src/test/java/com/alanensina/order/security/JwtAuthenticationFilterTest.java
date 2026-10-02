package com.alanensina.order.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = (servletRequest, servletResponse) -> { };

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void continuesWhenAuthorizationHeaderIsMissing() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void continuesWhenAuthorizationHeaderIsNotBearer() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void continuesWithoutAuthenticationWhenTokenIsInvalid() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");
        when(jwtService.validateToken("invalid")).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(jwtService).validateToken("invalid");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void authenticatesWhenTokenIsValid() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        when(jwtService.validateToken("valid-token")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(jwtService).validateToken("valid-token");
        assertEquals("customer-token", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
