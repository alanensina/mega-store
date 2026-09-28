package com.alanensina.customer;

import com.alanensina.customer.configurations.SwaggerConfig;
import com.alanensina.customer.dtos.*;
import com.alanensina.customer.exceptions.*;
import com.alanensina.customer.models.Customer;
import com.alanensina.customer.security.SecurityConfig;
import com.alanensina.customer.services.CustomerService;
import com.alanensina.customer.services.JwtService;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DomainAndConfigurationTest {

    @Test
    void shouldExerciseDtoAndCustomerAccessors() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        assertEquals("alice", new LoginDTO("alice", "password").username());
        assertEquals("token", new AuthenticationResponse(null, "token").token());
        assertEquals("alice@example.com", new CreateCustomerDTO("Alice", "alice@example.com", "alice", "Password123").email());
        assertEquals(id, new UpdateCustomerDTO(id, "Alice", "alice@example.com", "alice", "Password123").id());
        assertEquals("Alice", new CustomerDTO(id, "Alice", "alice@example.com", "alice", now, now).name());

        Customer customer = new Customer();
        customer.setId(id);
        customer.setVersion(2);
        customer.setName("Alice");
        customer.setEmail("alice@example.com");
        customer.setUsername("alice");
        customer.setPassword("hash");
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        Customer copy = Customer.builder().id(id).version(2).name("Alice").email("alice@example.com")
                .username("alice").password("hash").createdAt(now).updatedAt(now).build();
        assertEquals(copy, customer);
        assertEquals(2, customer.getVersion());
        assertNotNull(customer.toString());
        assertEquals(customer, customer);
    }

    @Test
    void shouldBuildSwaggerDefinition() throws Exception {
        SwaggerConfig config = new SwaggerConfig();
        setField(config, "title", "customer-api");
        setField(config, "version", "1.0");

        OpenAPI api = config.customOpenAPI();

        assertEquals("customer-api", api.getInfo().getTitle());
        assertEquals("1.0", api.getInfo().getVersion());
        assertEquals("JWT", api.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat());
    }

    @Test
    void shouldCreateJwtFilterBean() {
        JwtService jwtService = mock(JwtService.class);
        CustomerService customerService = mock(CustomerService.class);
        SecurityConfig config = new SecurityConfig(jwtService, customerService);

        assertNotNull(config.jwtAuthenticationFilter());
    }

    @Test
    void shouldInvokeApplicationMain() {
        assertNotNull(new CustomerApplication());
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            CustomerApplication.main(new String[]{"--test"});
            spring.verify(() -> SpringApplication.run(CustomerApplication.class, "--test"));
        }
    }

    @Test
    void shouldCreateExceptionMessages() {
        UUID id = UUID.randomUUID();
        assertTrue(new CustomerNotFoundException(id).getMessage().contains(id.toString()));
        assertTrue(new CustomerNotFoundException("alice").getMessage().contains("alice"));
        assertTrue(new CustomerUsernameOrEmailAlreadyExistsException("alice", "a@e.com").getMessage().contains("a@e.com"));
        assertTrue(new SecurityFilterChainFailedException("failure").getMessage().contains("failure"));
        assertEquals("Password hashing failed due to an internal error", new PasswordHashingFailedException().getMessage());
        assertTrue(new PasswordMatchingFailedException("failure").getMessage().contains("failure"));
        assertTrue(new InvalidCredentialsException("alice").getMessage().contains("alice"));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
