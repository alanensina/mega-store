package com.alanensina.customer.exceptions;

import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates optimistic-lock conflicts into 409 so clients can reload the resource and retry.
 */
@RestControllerAdvice
public class ConcurrencyExceptionHandler {

    static final String CONFLICT_DETAIL =
            "The resource was modified by another request. Reload it and retry.";

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ProblemDetail handleOptimisticLockConflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, CONFLICT_DETAIL);
    }
}
