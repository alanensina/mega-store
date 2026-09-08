package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReservationAlreadyCancelledException extends RuntimeException {

    public ReservationAlreadyCancelledException(UUID id) {
        super("Reservation already cancelled with id: " + id);
    }
}
