package com.alanensina.inventory.exceptions;

import com.alanensina.inventory.enums.StockOperation;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStockOperationException extends RuntimeException {

    public InvalidStockOperationException(StockOperation operation) {
        super("Invalid stock operation: " + operation);
    }
}
