package com.alanensina.order.services;

import com.alanensina.order.dtos.NotificationDTO;
import com.alanensina.order.dtos.OrderEventDTO;
import com.alanensina.order.models.OutboxEvent;
import com.alanensina.order.repositories.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Qualifier("ordersTopic")
    private final NewTopic ordersTopic;

    @Qualifier("notificationTopic")
    private final NewTopic notificationTopic;

    public void addOrderEvent(OrderEventDTO event) {
        addEvent(ordersTopic.name(), event.cartId().toString(), event);
    }

    public void addNotificationEvent(NotificationDTO event) {
        addEvent(notificationTopic.name(), event.orderId().toString(), event);
    }

    private <T> void addEvent(String topic, String key, T event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setTopic(topic);
        outboxEvent.setEventKey(key);
        outboxEvent.setPayload(toJson(event));
        outboxEvent.setCreatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        outboxEvent.setAttempts(0);
        repository.save(outboxEvent);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
    }
}
