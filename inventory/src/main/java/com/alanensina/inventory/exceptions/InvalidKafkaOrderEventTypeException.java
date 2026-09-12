package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidKafkaOrderEventTypeException extends RuntimeException {

    public InvalidKafkaOrderEventTypeException(String operation) {
        super("Invalid order event type: " + operation);
    }
}
