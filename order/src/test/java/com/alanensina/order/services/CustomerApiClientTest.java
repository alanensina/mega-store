package com.alanensina.order.services;

import com.alanensina.order.dtos.CustomerDTO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerApiClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchCustomerReturnsCustomerAndForwardsAuthorization() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = startServer(exchange -> {
            assertEquals("Bearer token", exchange.getRequestHeaders().getFirst("Authorization"));
            calls.incrementAndGet();
            respond(exchange, 200, """
                    {"id":"00000000-0000-0000-0000-000000000001","name":"Alice",
                    "email":"alice@example.com","username":"alice"}""");
        });
        CustomerApiClient client = client(3, 1000, 1000, 4);

        CustomerDTO result = client.fetchCustomer(UUID.randomUUID(), "Bearer token");

        assertEquals("Alice", result.name());
        assertEquals(1, calls.get());
    }

    @Test
    void fetchCustomerRetriesFailuresAndSucceedsWithoutAuthorization() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = startServer(exchange -> {
            if (calls.incrementAndGet() == 1) {
                respond(exchange, 503, "unavailable");
            } else {
                respond(exchange, 200, """
                        {"id":"00000000-0000-0000-0000-000000000001","name":"Alice",
                        "email":"alice@example.com","username":"alice"}""");
            }
        });
        CustomerApiClient client = client(2, 1, 1, 4);

        CustomerDTO result = client.fetchCustomer(UUID.randomUUID(), null);

        assertEquals("Alice", result.name());
        assertEquals(2, calls.get());
    }

    @Test
    void fetchCustomerOpensCircuitAfterRepeatedFailures() throws Exception {
        server = startServer(exchange -> respond(exchange, 503, "unavailable"));
        CustomerApiClient client = client(1, 1, 1, 1);
        UUID customerId = UUID.randomUUID();

        assertThrows(RestClientException.class, () -> client.fetchCustomer(customerId, null));
        RestClientException exception = assertThrows(
                RestClientException.class,
                () -> client.fetchCustomer(customerId, null));

        assertEquals("Customer API circuit breaker is open.", exception.getMessage());
    }

    @Test
    void fetchCustomerRejectsEmptyResponse() throws Exception {
        server = startServer(exchange -> respond(exchange, 200, ""));
        CustomerApiClient client = new CustomerApiClient(
                "http://localhost:1", 1, 1, 1, 4, 30_000, 20);
        UUID customerId = UUID.randomUUID();

        RestClientException exception = assertThrows(
                RestClientException.class,
                () -> client.fetchCustomer(customerId, null));

        assertEquals(true, exception.getMessage().startsWith("Failed to fetch customer"));
    }

    @Test
    void fetchCustomerRejectsNullResponseBody() throws Exception {
        CustomerApiClient client = new CustomerApiClient(
                "http://localhost:1", 1, 1, 1, 4, 30_000, 20);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec request = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestWithUri = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(request);
        when(request.uri(any(String.class), any(Object.class))).thenReturn(requestWithUri);
        when(requestWithUri.headers(any())).thenReturn(requestWithUri);
        when(requestWithUri.retrieve()).thenReturn(response);
        when(response.body(CustomerDTO.class)).thenReturn(null);

        var field = CustomerApiClient.class.getDeclaredField("restClient");
        field.setAccessible(true);
        field.set(client, restClient);
        UUID customerId = UUID.randomUUID();

        RestClientException exception = assertThrows(
                RestClientException.class,
                () -> client.fetchCustomer(customerId, null));

        assertEquals(true, exception.getCause().getMessage().contains("empty response"));
    }

    @Test
    void fetchCustomerWrapsTimeout() throws Exception {
        CountDownLatch responseGate = new CountDownLatch(1);
        server = startServer(exchange -> {
            try {
                responseGate.await();
                respond(exchange, 200, "{}");
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });
        CustomerApiClient client = client(1, 1, 1, 1);
        UUID customerId = UUID.randomUUID();

        try {
            RestClientException exception = assertThrows(
                    RestClientException.class,
                    () -> client.fetchCustomer(customerId, null));

            assertEquals(true, exception.getCause() instanceof RestClientException);
        } finally {
            responseGate.countDown();
        }
    }

    @Test
    void fetchCustomerFailsWhenRetryIsInterrupted() throws Exception {
        server = startServer(exchange -> respond(exchange, 503, "unavailable"));
        CustomerApiClient client = client(2, 100, 100, 4);
        UUID customerId = UUID.randomUUID();
        Thread.currentThread().interrupt();

        try {
            RestClientException exception = assertThrows(
                    RestClientException.class,
                    () -> client.fetchCustomer(customerId, null));
            assertEquals("Customer retry was interrupted.", exception.getMessage());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void fetchCustomerWrapsUnexpectedAsyncErrors() throws Exception {
        CustomerApiClient client = new CustomerApiClient(
                "http://localhost:1", 1, 1, 1, 4, 30_000, 20);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec request = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestWithUri = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(request);
        when(request.uri(any(String.class), any(Object.class))).thenReturn(requestWithUri);
        when(requestWithUri.headers(any())).thenReturn(requestWithUri);
        when(requestWithUri.retrieve()).thenReturn(response);
        when(response.body(CustomerDTO.class)).thenThrow(new AssertionError("unexpected"));

        var field = CustomerApiClient.class.getDeclaredField("restClient");
        field.setAccessible(true);
        field.set(client, restClient);
        UUID customerId = UUID.randomUUID();

        RestClientException exception = assertThrows(
                RestClientException.class,
                () -> client.fetchCustomer(customerId, null));

        assertEquals(true, exception.getMessage().startsWith("Failed to fetch customer"));
        assertEquals("Unexpected error fetching customer details.", exception.getCause().getMessage());
    }

    private CustomerApiClient client(int attempts, long initialDelay, long maxDelay, int threshold) {
        return new CustomerApiClient(
                "http://localhost:" + server.getAddress().getPort(),
                attempts, initialDelay, maxDelay, threshold, 30_000, 20);
    }

    private HttpServer startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/customer", handler);
        server.start();
        return server;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
