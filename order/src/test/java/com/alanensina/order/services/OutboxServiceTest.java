package com.alanensina.order.services;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.dtos.NotificationDTO;
import com.alanensina.order.dtos.OrderEventDTO;
import com.alanensina.order.dtos.ReservationItem;
import com.alanensina.order.enums.NotificationType;
import com.alanensina.order.enums.OrderEventType;
import com.alanensina.order.enums.ReservationStatus;
import com.alanensina.order.models.OutboxEvent;
import com.alanensina.order.repositories.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxServiceTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final NewTopic ordersTopic = new NewTopic("orders", 1, (short) 1);
    private final NewTopic notificationTopic = new NewTopic("notifications", 1, (short) 1);
    private final OutboxService outboxService =
            new OutboxService(repository, objectMapper, ordersTopic, notificationTopic);

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
    }

    @Test
    void addOrderEventSerializesAndStoresTopicAndEventKey() throws JsonProcessingException {
        OrderEventDTO event = new OrderEventDTO(
                OrderEventType.CREATE,
                orderId,
                List.of(new ReservationItem(UUID.randomUUID(), 2)),
                ReservationStatus.AWAITING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now(),
                UUID.randomUUID());
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"event\":\"order\"}");

        outboxService.addOrderEvent(event);

        OutboxEvent stored = capturedEvent();
        assertEquals("orders", stored.getTopic());
        assertEquals(orderId.toString(), stored.getEventKey());
        assertEquals("{\"event\":\"order\"}", stored.getPayload());
        assertEquals(0, stored.getAttempts());
    }

    @Test
    void addNotificationEventSerializesAndStoresNotificationTopic() throws JsonProcessingException {
        NotificationDTO event = new NotificationDTO(
                "Alice",
                "alice@example.com",
                orderId,
                new CartDTO(orderId, UUID.randomUUID(), BigDecimal.TEN,
                        ReservationStatus.PAID, null, null, List.of()),
                NotificationType.NEW_ORDER);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"event\":\"notification\"}");

        outboxService.addNotificationEvent(event);

        OutboxEvent stored = capturedEvent();
        assertEquals("notifications", stored.getTopic());
        assertEquals(orderId.toString(), stored.getEventKey());
        assertEquals("{\"event\":\"notification\"}", stored.getPayload());
    }

    @Test
    void serializationFailureIsWrappedWithItsCause() throws JsonProcessingException {
        JsonProcessingException cause = new JsonProcessingException("invalid event") {};
        when(objectMapper.writeValueAsString(any())).thenThrow(cause);
        OrderEventDTO event = new OrderEventDTO(
                OrderEventType.CREATE, orderId, List.of(), ReservationStatus.AWAITING_PAYMENT,
                LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> outboxService.addOrderEvent(event));

        assertEquals("Could not serialize outbox event", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    private OutboxEvent capturedEvent() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
