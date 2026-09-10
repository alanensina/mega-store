package com.alanensina.customer.services;

import com.alanensina.customer.dtos.CustomerDTO;
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

    public CustomerDTO login(@Valid LoginDTO loginDTO) {
        var customer = customerService.getCustomerByUsername(loginDTO.username());

        if(validatePassword(loginDTO, customer)) {
            return customerService.toDTO(customer);
        }

        log.warn("Invalid login attempt for username: {}", loginDTO.username());
        throw new InvalidCredentialsException(loginDTO.username());
    }

    private boolean validatePassword(LoginDTO loginDTO, Customer customer) {
        return passwordEncryptionService.matchPassword(loginDTO.password(), customer.getPassword());
    }
}
