package com.alanensina.customer.services;

import com.alanensina.customer.dtos.AuthenticationResponse;
import com.alanensina.customer.dtos.CustomerDTO;
import com.alanensina.customer.dtos.LoginDTO;
import com.alanensina.customer.exceptions.InvalidCredentialsException;
import com.alanensina.customer.models.Customer;
import com.alanensina.customer.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private PasswordEncryptionService passwordEncryptionService;

    @Mock
    private CustomerService customerService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void loginShouldReturnAuthenticationResponseWhenPasswordIsValid() {
        LoginDTO login = new LoginDTO("alice", "Password123");
        Customer customer = customer();
        CustomerDTO customerDTO = new CustomerDTO(
                customer.getId(), customer.getName(), customer.getEmail(), customer.getUsername(),
                customer.getCreatedAt(), customer.getUpdatedAt(), false);

        when(customerService.getCustomerByUsernameToAuthentication("alice")).thenReturn(customer);
        when(passwordEncryptionService.matchPassword("Password123", "hashed-password")).thenReturn(true);
        when(customerService.toDTO(customer)).thenReturn(customerDTO);
        when(jwtService.generateToken(customer)).thenReturn("token");

        AuthenticationResponse result = authenticationService.login(login);

        assertSame(customerDTO, result.customer());
        assertEquals("token", result.token());
        verify(jwtService).generateToken(customer);
    }

    @Test
    void loginShouldThrowWhenPasswordIsInvalid() {
        LoginDTO login = new LoginDTO("alice", "wrong");
        when(customerService.getCustomerByUsernameToAuthentication("alice")).thenReturn(customer());
        when(passwordEncryptionService.matchPassword("wrong", "hashed-password")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(login));

        assertEquals("Invalid credentials for username 'alice'", exception.getMessage());
        verify(customerService, never()).toDTO(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginShouldThrowWhenCustomerDoesNotExist() {
        LoginDTO login = new LoginDTO("alice", "Password123");
        when(customerService.getCustomerByUsernameToAuthentication("alice")).thenReturn(null);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(login));

        assertEquals("Invalid credentials for username 'alice'", exception.getMessage());
        verifyNoInteractions(passwordEncryptionService, jwtService);
        verify(customerService, never()).toDTO(any());
    }

    private Customer customer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@email.com")
                .username("alice")
                .password("hashed-password")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
