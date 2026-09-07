package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.CONFLICT)
public class ProductNameAlreadyExistsException extends RuntimeException {

    public ProductNameAlreadyExistsException(String name) {
        super("Product with name '" + name + "' already exists");
    }
}
