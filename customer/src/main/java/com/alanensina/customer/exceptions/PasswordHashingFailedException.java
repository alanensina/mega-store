package com.alanensina.customer.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PasswordHashingFailedException extends RuntimeException {

    public PasswordHashingFailedException() {
        super("Password hashing failed due to an internal error");
    }

}
