package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ProductNotFoundInStockException extends RuntimeException {

    public ProductNotFoundInStockException(UUID productid) {
        super("Product with ID " + productid + " not found in stock.");
    }
}
