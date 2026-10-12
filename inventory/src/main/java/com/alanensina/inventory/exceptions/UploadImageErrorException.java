package com.alanensina.inventory.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class UploadImageErrorException extends RuntimeException {

    public UploadImageErrorException() {
        super("Could not upload product image to S3 bucket.");
    }
}
