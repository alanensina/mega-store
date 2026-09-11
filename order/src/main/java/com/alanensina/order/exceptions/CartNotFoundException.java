package com.alanensina.order.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(UUID id) {
        super("Cart with ID " + id + " not found.");
    }
}
