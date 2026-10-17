package com.alanensina.order.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class IdempotencyRequestInProgressException extends RuntimeException {

    public IdempotencyRequestInProgressException(String requestKey) {
        super("Idempotency key '" + requestKey + "' is currently being processed.");
    }
}
