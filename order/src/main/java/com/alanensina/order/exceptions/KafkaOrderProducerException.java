package com.alanensina.order.exceptions;

import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class KafkaOrderProducerException extends RuntimeException {

    public KafkaOrderProducerException(String error) {
        super("Error producing order event: " + error);
    }
}
