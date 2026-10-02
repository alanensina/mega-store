package com.alanensina.order.producers;

import com.alanensina.order.dtos.NotificationDTO;
import com.alanensina.order.dtos.OrderEventDTO;
import com.alanensina.order.exceptions.KafkaOrderProducerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProducerTest {

    private final NewTopic ordersTopic = new NewTopic("orders", 1, (short) 1);
    private final NewTopic notificationTopic = new NewTopic("notifications", 1, (short) 1);
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final OrderProducer producer =
            new OrderProducer(ordersTopic, notificationTopic, kafkaTemplate, objectMapper);

    @Test
    void sendsOrderEventsWithCartIdAsKey() throws JsonProcessingException {
        OrderEventDTO event = mock(OrderEventDTO.class);
        when(event.cartId()).thenReturn(java.util.UUID.randomUUID());
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"type\":\"order\"}");

        producer.sendOrderMessage(event);

        verify(kafkaTemplate).send("orders", event.cartId().toString(), "{\"type\":\"order\"}");
    }

    @Test
    void sendsNotificationEventsWithOrderIdAsKey() throws JsonProcessingException {
        NotificationDTO event = mock(NotificationDTO.class);
        when(event.orderId()).thenReturn(java.util.UUID.randomUUID());
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"type\":\"notification\"}");

        producer.sendNotificationMessage(event);

        verify(kafkaTemplate).send("notifications", event.orderId().toString(), "{\"type\":\"notification\"}");
    }

    @Test
    void wrapsSerializationFailures() throws JsonProcessingException {
        OrderEventDTO event = mock(OrderEventDTO.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("serialization failed") {});

        KafkaOrderProducerException exception = assertThrows(
                KafkaOrderProducerException.class, () -> producer.sendOrderMessage(event));

        assertEquals("Error producing order event: serialization failed", exception.getMessage());
    }

    @Test
    void wrapsKafkaSendFailures() throws JsonProcessingException {
        OrderEventDTO event = mock(OrderEventDTO.class);
        UUID orderId = UUID.randomUUID();
        when(event.cartId()).thenReturn(orderId);
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(kafkaTemplate.send("orders", orderId.toString(), "{}"))
                .thenThrow(new IllegalStateException("broker failed"));

        KafkaOrderProducerException exception = assertThrows(
                KafkaOrderProducerException.class, () -> producer.sendOrderMessage(event));

        assertEquals("Error producing order event: broker failed", exception.getMessage());
        verify(kafkaTemplate).send("orders", orderId.toString(), "{}");
    }
}
