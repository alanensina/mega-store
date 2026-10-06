package com.alanensina.inventory.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrencyExceptionHandlerTest {

    private final ConcurrencyExceptionHandler handler = new ConcurrencyExceptionHandler();

    @Test
    void shouldAnswerConflictForOptimisticLockFailures() {
        ProblemDetail problem = handler.handleOptimisticLockConflict();

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals(ConcurrencyExceptionHandler.CONFLICT_DETAIL, problem.getDetail());
    }

    @Test
    void shouldHandleSpringAndJpaOptimisticLockExceptions() throws NoSuchMethodException {
        Method method = ConcurrencyExceptionHandler.class.getMethod("handleOptimisticLockConflict");

        Set<String> handled = Arrays.stream(method.getAnnotation(ExceptionHandler.class).value())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("OptimisticLockingFailureException", "OptimisticLockException"), handled);
    }
}
