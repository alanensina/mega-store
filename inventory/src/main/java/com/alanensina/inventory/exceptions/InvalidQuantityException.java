package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException() {
        super("Stock quantity must be greater than zero");
    }
}
