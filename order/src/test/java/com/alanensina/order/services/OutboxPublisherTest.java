package com.alanensina.order.services;

import com.alanensina.order.models.OutboxEvent;
import com.alanensina.order.repositories.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate);

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void publishMarksEventsAsPublishedAndClearsPreviousError() {
        OutboxEvent event = event();
        event.setLastError("old failure");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send("orders", "order-key", "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        assertNotNull(event.getPublishedAt());
        assertNull(event.getLastError());
        verify(repository).save(event);
    }

    @Test
    void publishRecordsFailuresAndContinuesWithFollowingEvents() {
        OutboxEvent failedEvent = event();
        OutboxEvent succeedingEvent = event();
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(failedEvent, succeedingEvent));
        when(kafkaTemplate.send("orders", "order-key", "payload"))
                .thenReturn(CompletableFuture.failedFuture(new ExecutionException("broker unavailable", null)),
                        CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        assertEquals(1, failedEvent.getAttempts());
        assertEquals("java.util.concurrent.ExecutionException: broker unavailable", failedEvent.getLastError());
        assertNull(failedEvent.getPublishedAt());
        assertNotNull(succeedingEvent.getPublishedAt());
        verify(repository).save(failedEvent);
        verify(repository).save(succeedingEvent);
    }

    @Test
    void publishStopsAfterAnInterruptedSendAndRestoresInterruptStatus() throws Exception {
        OutboxEvent interruptedEvent = event();
        OutboxEvent followingEvent = event();
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(interruptedEvent, followingEvent));
        when(kafkaTemplate.send("orders", "order-key", "payload")).thenReturn(future);
        when(future.get(10, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        publisher.publishPendingEvents();

        assertEquals(1, interruptedEvent.getAttempts());
        assertEquals("interrupted", interruptedEvent.getLastError());
        assertNull(interruptedEvent.getPublishedAt());
        assertTrue(Thread.currentThread().isInterrupted());
        verify(repository).save(interruptedEvent);
        verify(repository, never()).save(followingEvent);
    }

    @Test
    void publishDoesNothingWhenThereAreNoPendingEvents() {
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(repository, never()).save(any(OutboxEvent.class));
    }

    private OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setTopic("orders");
        event.setEventKey("order-key");
        event.setPayload("payload");
        return event;
    }
}
