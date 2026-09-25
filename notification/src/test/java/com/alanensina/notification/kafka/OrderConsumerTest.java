package com.alanensina.notification.kafka;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.enums.NotificationType;
import com.alanensina.notification.exceptions.KafkaErrorException;
import com.alanensina.notification.services.NotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationDeliveryService deliveryService;

    @Mock
    private JsonNode jsonNode;

    @Test
    void consumeShouldDeserializeAndDeliverNotification() {
        NotificationDTO event = new NotificationDTO(
                "Alice", "alice@example.com", UUID.randomUUID(), null, NotificationType.NEW_ORDER);
        OrderConsumer consumer = new OrderConsumer(objectMapper, deliveryService);
        when(objectMapper.readValue("message", NotificationDTO.class)).thenReturn(event);

        consumer.consume("message");

        verify(deliveryService).deliver(event);
    }

    @Test
    void consumeShouldRejectNotificationWithoutType() {
        NotificationDTO event = new NotificationDTO(
                "Alice", "alice@example.com", UUID.randomUUID(), null, null);
        OrderConsumer consumer = new OrderConsumer(objectMapper, deliveryService);
        when(objectMapper.readValue("message", NotificationDTO.class)).thenReturn(event);
        stubOrderIdLookup("order-id");

        KafkaErrorException exception = assertThrows(
                KafkaErrorException.class,
                () -> consumer.consume("message"));

        assertEquals(
                "Error processing Kafka message: Invalid notification event type: null",
                exception.getMessage());
    }

    @Test
    void consumeShouldWrapDeserializationErrors() {
        OrderConsumer consumer = new OrderConsumer(objectMapper, deliveryService);
        when(objectMapper.readValue("message", NotificationDTO.class))
                .thenThrow(new IllegalArgumentException("invalid json"));
        stubOrderIdLookup("order-id");

        KafkaErrorException exception = assertThrows(
                KafkaErrorException.class,
                () -> consumer.consume("message"));

        assertEquals(
                "Error processing Kafka message: invalid json",
                exception.getMessage());
    }

    @Test
    void consumeShouldWrapDeliveryErrors() {
        UUID orderId = UUID.randomUUID();
        NotificationDTO event = new NotificationDTO(
                "Alice", "alice@example.com", orderId, null, NotificationType.CANCEL);
        OrderConsumer consumer = new OrderConsumer(objectMapper, deliveryService);
        when(objectMapper.readValue("message", NotificationDTO.class)).thenReturn(event);
        doThrow(new IllegalStateException("delivery failed"))
                .when(deliveryService).deliver(event);
        stubOrderIdLookup(orderId.toString());

        KafkaErrorException exception = assertThrows(
                KafkaErrorException.class,
                () -> consumer.consume("message"));

        assertEquals(
                "Error processing Kafka message: delivery failed",
                exception.getMessage());
    }

    @Test
    void consumeShouldUseUnknownOrderIdWhenOrderIdCannotBeRead() {
        OrderConsumer consumer = new OrderConsumer(objectMapper, deliveryService);
        when(objectMapper.readValue("message", NotificationDTO.class))
                .thenThrow(new IllegalArgumentException("invalid json"));
        when(objectMapper.readTree("message"))
                .thenThrow(new IllegalStateException("invalid tree"))
                .thenReturn(jsonNode);
        when(jsonNode.path("orderId")).thenReturn(jsonNode);
        when(jsonNode.asText("unknown")).thenReturn("unknown");

        assertThrows(KafkaErrorException.class, () -> consumer.consume("message"));
    }

    private void stubOrderIdLookup(String orderId) {
        when(objectMapper.readTree("message")).thenReturn(jsonNode);
        when(jsonNode.path("orderId")).thenReturn(jsonNode);
        when(jsonNode.asText("unknown")).thenReturn(orderId);
    }
}
