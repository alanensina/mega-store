package com.alanensina.customer.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.CONFLICT)
public class CustomerUsernameOrEmailAlreadyExistsException extends RuntimeException {

    public CustomerUsernameOrEmailAlreadyExistsException(String username, String email) {
        super(String.format("Customer with username '%s' or email '%s' already exists", username, email));
    }
}
