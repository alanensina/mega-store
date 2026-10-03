package com.alanensina.inventory.consumers;

import com.alanensina.inventory.dtos.OrderEventDTO;
import com.alanensina.inventory.exceptions.KafkaErrorException;
import com.alanensina.inventory.services.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class OrderConsumer {

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message){
        try {
            var event = objectMapper.readValue(message, OrderEventDTO.class);
            if (event.eventId() == null) {
                var legacyEventId = UUID.nameUUIDFromBytes(message.getBytes(StandardCharsets.UTF_8));
                log.warn("Order event has no eventId; using a deterministic ID for this legacy message");
                event = new OrderEventDTO(
                        event.eventType(),
                        event.cartId(),
                        event.items(),
                        event.status(),
                        event.createdAt(),
                        event.updatedAt(),
                        legacyEventId
                );
            }
            reservationService.process(event);
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
            throw new KafkaErrorException(e.getMessage());
        }
    }
}
