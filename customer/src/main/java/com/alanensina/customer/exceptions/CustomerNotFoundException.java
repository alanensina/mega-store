package com.alanensina.customer.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID id) {
        super(String.format("Customer with id '%s' not found", id));
    }

    public CustomerNotFoundException(String username) {
        super(String.format("Customer with username '%s' not found", username));
    }

}
