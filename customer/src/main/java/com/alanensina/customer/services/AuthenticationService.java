package com.alanensina.customer.services;

import com.alanensina.customer.dtos.AuthenticationResponse;
import com.alanensina.customer.dtos.LoginDTO;
import com.alanensina.customer.exceptions.InvalidCredentialsException;
import com.alanensina.customer.models.Customer;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthenticationService {

    private final PasswordEncryptionService passwordEncryptionService;
    private final CustomerService customerService;
    private final JwtService jwtService;

    public AuthenticationResponse login(@Valid LoginDTO loginDTO) {
        var customer = customerService.getCustomerByUsernameToAuthentication(loginDTO.username());

        if(customer == null) {
            log.warn("Invalid login attempt for username: {}", loginDTO.username());
            throw new InvalidCredentialsException(loginDTO.username());
        }

        if (validatePassword(loginDTO, customer)) {
            var dto = customerService.toDTO(customer);
            var token = jwtService.generateToken(customer);
            return new AuthenticationResponse(dto, token);
        }

        log.warn("Invalid login attempt for username: {}", loginDTO.username());
        throw new InvalidCredentialsException(loginDTO.username());
    }

    private boolean validatePassword(LoginDTO loginDTO, Customer customer) {
        return passwordEncryptionService.matchPassword(loginDTO.password(), customer.getPassword());
    }
}
