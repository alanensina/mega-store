package com.alanensina.order.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class CustomerDetailsFetchException extends RuntimeException {

    public CustomerDetailsFetchException(UUID customerId, Throwable cause) {
        super("Error fetching customer details for ID " + customerId + ".", cause);
    }
}
