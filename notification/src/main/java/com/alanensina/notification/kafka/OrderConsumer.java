package com.alanensina.notification.kafka;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.exceptions.InvalidKafkaNotificationEventTypeException;
import com.alanensina.notification.exceptions.KafkaErrorException;
import com.alanensina.notification.services.NotificationDeliveryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private final ObjectMapper objectMapper;
    private final NotificationDeliveryService deliveryService;

    public OrderConsumer(ObjectMapper objectMapper, NotificationDeliveryService deliveryService) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message){
        try {
            var event = objectMapper.readValue(message, NotificationDTO.class);

            if (event.notificationType() == null) {
                throw new InvalidKafkaNotificationEventTypeException("null");
            }
            deliveryService.deliver(event);
        } catch (RuntimeException e) {
            log.error("Error processing notification for order {}", safeOrderId(message), e);
            throw new KafkaErrorException(e.getMessage());
        }
    }

    private String safeOrderId(String message) {
        try {
            return objectMapper.readTree(message).path("orderId").asText("unknown");
        } catch (RuntimeException e) {
            log.error("Error reading order id from notification", e);
            return "unknown";
        }
    }
}
