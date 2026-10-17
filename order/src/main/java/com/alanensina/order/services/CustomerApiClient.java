package com.alanensina.order.services;

import com.alanensina.order.dtos.CustomerDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CustomerApiClient {

    private final RestClient restClient;
    private final int maxAttempts;
    private final long retryInitialDelayMs;
    private final long retryMaxDelayMs;
    private final int circuitFailureThreshold;
    private final long circuitOpenDurationMs;
    private final long requestTimeoutMs;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openUntilEpochMs = new AtomicLong(0L);

    public CustomerApiClient(
            @Value("${order.customer.base-url:http://localhost:8081}") String baseUrl,
            @Value("${order.customer.retry.max-attempts:3}") int maxAttempts,
            @Value("${order.customer.retry.initial-delay-ms:200}") long retryInitialDelayMs,
            @Value("${order.customer.retry.max-delay-ms:2000}") long retryMaxDelayMs,
            @Value("${order.customer.circuit.failure-threshold:4}") int circuitFailureThreshold,
            @Value("${order.customer.circuit.open-duration-ms:30000}") long circuitOpenDurationMs,
            @Value("${order.customer.timeout-ms:2000}") long requestTimeoutMs
    ) {
        this.restClient = RestClient.create(baseUrl);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryInitialDelayMs = Math.max(1, retryInitialDelayMs);
        this.retryMaxDelayMs = Math.max(this.retryInitialDelayMs, retryMaxDelayMs);
        this.circuitFailureThreshold = Math.max(1, circuitFailureThreshold);
        this.circuitOpenDurationMs = Math.max(1000, circuitOpenDurationMs);
        this.requestTimeoutMs = Math.max(500, requestTimeoutMs);
    }

    public CustomerDTO fetchCustomer(UUID customerId, String authorizationHeader) {
        if (isCircuitOpen()) {
            throw new RestClientException("Customer API circuit breaker is open.");
        }

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                var customer = callCustomerApiWithTimeout(customerId, authorizationHeader);
                onSuccess();
                return customer;
            } catch (RuntimeException ex) {
                lastError = ex;
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        onFailure();
        throw new RestClientException(
                "Failed to fetch customer " + customerId + " after " + maxAttempts + " attempts.",
                lastError);
    }

    private CustomerDTO callCustomerApiWithTimeout(UUID customerId, String authorizationHeader) {
        try {
            return CompletableFuture.supplyAsync(() -> requestCustomer(customerId, authorizationHeader))
                    .orTimeout(requestTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof TimeoutException timeoutException) {
                throw new RestClientException("Timed out fetching customer details.", timeoutException);
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RestClientException("Unexpected error fetching customer details.", cause);
        }
    }

    private CustomerDTO requestCustomer(UUID customerId, String authorizationHeader) {
        var customer = restClient.get()
                .uri("/api/customer/{id}", customerId)
                .headers(headers -> {
                    if (authorizationHeader != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                    }
                })
                .retrieve()
                .body(CustomerDTO.class);

        if (customer == null) {
            throw new RestClientException("Customer API returned an empty response.");
        }
        return customer;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(backoffDelayMs(attempt));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Customer retry was interrupted.", ex);
        }
    }

    private long backoffDelayMs(int attempt) {
        long shift = Math.max(0, attempt - 1);
        long delay = retryInitialDelayMs << Math.min(shift, 30);
        return Math.min(delay, retryMaxDelayMs);
    }

    private boolean isCircuitOpen() {
        long now = System.currentTimeMillis();
        return now < openUntilEpochMs.get();
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        openUntilEpochMs.set(0L);
    }

    private void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= circuitFailureThreshold) {
            openUntilEpochMs.set(System.currentTimeMillis() + circuitOpenDurationMs);
        }
    }
}
