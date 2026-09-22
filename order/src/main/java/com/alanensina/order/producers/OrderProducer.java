package com.alanensina.order.producers;

import com.alanensina.order.dtos.NotificationDTO;
import com.alanensina.order.dtos.OrderEventDTO;
import com.alanensina.order.enums.TopicEventType;
import com.alanensina.order.exceptions.KafkaOrderProducerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.alanensina.order.enums.TopicEventType.NOTIFICATION;
import static com.alanensina.order.enums.TopicEventType.ORDER;

@Service
@Slf4j
public class OrderProducer {

    private final NewTopic ordersTopic;
    private final NewTopic notificationTopic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderProducer(
            @Qualifier("ordersTopic") NewTopic ordersTopic,
            @Qualifier("notificationTopic") NewTopic notificationTopic,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.ordersTopic = ordersTopic;
        this.notificationTopic = notificationTopic;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderMessage(OrderEventDTO event) {
        sendToTopic(ordersTopic, event, ORDER);
    }

    public void sendNotificationMessage(NotificationDTO event) {
        sendToTopic(notificationTopic, event, NOTIFICATION);
    }

    private <T> void sendToTopic(NewTopic topic, T event, TopicEventType eventType) {
        log.info("Sending {} event to topic {} -> {}", eventType.name(), topic.name(), event);

        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event instanceof OrderEventDTO orderEvent
                    ? orderEvent.cartId().toString()
                    : ((NotificationDTO) event).orderId().toString();
            kafkaTemplate.send(topic.name(), key, payload);
        } catch (Exception e) {
            log.error("Error to send a {} event to topic {} -> {}", eventType.name(), topic.name(), event, e);
            throw new KafkaOrderProducerException(e.getMessage());
        }

        log.info("{} event sent successfully to topic {} -> {}", eventType.name(), topic.name(), event);
    }
}
