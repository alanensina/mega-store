package com.alanensina.customer.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SecurityFilterChainFailedException extends RuntimeException {

    public SecurityFilterChainFailedException(String error) {
        super("Failed to configure Spring Security Chain: " + error);
    }
}
