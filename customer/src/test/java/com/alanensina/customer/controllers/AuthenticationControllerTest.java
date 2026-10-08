package com.alanensina.customer.controllers;

import com.alanensina.customer.dtos.AuthenticationResponse;
import com.alanensina.customer.dtos.CustomerDTO;
import com.alanensina.customer.dtos.LoginDTO;
import com.alanensina.customer.services.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController controller;

    @Test
    void shouldReturnAuthenticationServiceResult() {
        LoginDTO login = new LoginDTO("alice", "Password123");
        AuthenticationResponse response = new AuthenticationResponse(
                new CustomerDTO(UUID.randomUUID(), "Alice", "alice@example.com", "alice", null, null, false),
                "token");
        when(authenticationService.login(login)).thenReturn(response);

        assertSame(response, controller.login(login).getBody());
        verify(authenticationService).login(login);
    }
}
