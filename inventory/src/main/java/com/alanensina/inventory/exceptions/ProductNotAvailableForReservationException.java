package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ProductNotAvailableForReservationException extends RuntimeException {

    public ProductNotAvailableForReservationException(UUID id) {
        super("Product with ID " + id + " not available in stock.");
    }
}
