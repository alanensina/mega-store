package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InsufficientStockForReservationException extends RuntimeException {

    public InsufficientStockForReservationException(UUID productId) {
        super("Insufficient stock for product with ID " + productId + " to fulfill the reservation.");
    }
}
