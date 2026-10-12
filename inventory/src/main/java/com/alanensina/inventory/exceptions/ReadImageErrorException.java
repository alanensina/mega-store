package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ReadImageErrorException extends RuntimeException {

    public ReadImageErrorException() {
        super("Could not read product image from S3 bucket.");
    }
}
