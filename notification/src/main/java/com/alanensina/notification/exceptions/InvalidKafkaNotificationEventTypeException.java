package com.alanensina.notification.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidKafkaNotificationEventTypeException extends RuntimeException {

    public InvalidKafkaNotificationEventTypeException(String operation) {
        super("Invalid notification event type: " + operation);
    }
}
