package com.alanensina.notification.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class KafkaDataIntegrityViolationException extends RuntimeException {

    public KafkaDataIntegrityViolationException(String error) {
        super("Data Integrity violation: " + error);
    }
}
