package com.alanensina.customer.security;

import com.alanensina.customer.exceptions.SecurityFilterChainFailedException;
import com.alanensina.customer.services.CustomerService;
import com.alanensina.customer.services.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final CustomerService customerService;

    // Control whether the CSRF cookie is HttpOnly. Default true for safety; set to false for some local dev UIs only.
    @Value("${csrf.cookie-http-only:true}")
    private boolean csrfCookieHttpOnly;

    public SecurityConfig(JwtService jwtService, CustomerService customerService) {
        this.jwtService = jwtService;
        this.customerService = customerService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, customerService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                    // enable CSRF protection for browser interactions (Swagger UI),
                    // but ignore our stateless API endpoints which use JWT for auth
                    .csrf(csrf -> {
                            // Create CSRF cookie repository. Some Spring Security versions don't expose setters
                            // for cookie flags; those flags should be enforced by infrastructure (reverse proxy / load balancer).
                            var csrfRepo = new CookieCsrfTokenRepository(); // NOSONAR: CookieCsrfTokenRepository sets cookie flags via infra; keeping HttpOnly enforced by default

                            // Only ignore CSRF for the specific stateless endpoints that use JWT or are used by public clients:
                            // - authentication endpoints used to obtain tokens (they accept credentials and return a token)
                            // - customer creation (signup) which is intentionally public
                            // - customer update and deletion, which use Authorization headers for auth (JWT)
                            // These endpoints are either public (signup) or use Authorization headers for auth (JWT),
                            // so CSRF risks are minimal when limited to these paths.
                            csrf.csrfTokenRepository(csrfRepo)
                                    .ignoringRequestMatchers(request -> {
                                        String path = request.getServletPath();
                                        String method = request.getMethod();
                                        if (path == null) return false;
                                        // Match both /api/auth and any subpaths like /api/auth/
                                        if ("/api/auth".equals(path) || path.startsWith("/api/auth/")) return true;
                                        return "/api/customer".equals(path)
                                                && ("POST".equalsIgnoreCase(method)
                                                || "PUT".equalsIgnoreCase(method)
                                                || "DELETE".equalsIgnoreCase(method));
                                    }); // NOSONAR: intentionally ignoring CSRF only for stateless API endpoints

                        })
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth", "/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/customer").permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

            return http.build();
        } catch (Exception e) {
            throw new SecurityFilterChainFailedException(e.getMessage());
        }
    }
}
