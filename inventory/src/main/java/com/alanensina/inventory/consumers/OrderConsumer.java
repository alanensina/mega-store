package com.alanensina.inventory.consumers;

import com.alanensina.inventory.dtos.OrderEventDTO;
import com.alanensina.inventory.enums.OrderEventType;
import com.alanensina.inventory.exceptions.InvalidKafkaOrderEventTypeException;
import com.alanensina.inventory.exceptions.KafkaErrorException;
import com.alanensina.inventory.services.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

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

            if (OrderEventType.CREATE == event.eventType()) {
                reservationService.create(event);
            }
            else if (OrderEventType.UPDATE == event.eventType()) {
                reservationService.update(event);
            }
            else if (OrderEventType.CANCEL == event.eventType()) {
                reservationService.cancel(event);
            }
            else {
                log.error("Unhandled event type: {}", event.eventType());
                throw new InvalidKafkaOrderEventTypeException(String.valueOf(event.eventType()));
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
            throw new KafkaErrorException(e.getMessage());
        }
    }
}
