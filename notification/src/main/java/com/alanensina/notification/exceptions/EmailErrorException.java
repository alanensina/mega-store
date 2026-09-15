package com.alanensina.notification.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EmailErrorException extends RuntimeException {

    public EmailErrorException(String error) {
        super("Error sending email: " + error);
    }
}
