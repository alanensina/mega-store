package com.alanensina.notification.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@AllArgsConstructor
@Slf4j
public class OrderConsumer {

    //private final ObjectMapper objectMapper;

//    @KafkaListener(
//            topics = "${spring.kafka.topic.name}",
//            groupId = "${spring.kafka.consumer.group-id}"
//    )
//    public void consume(String message){
//        try {
//            var event = objectMapper.readValue(message, OrderEventDTO.class);
//
//            switch(event.eventType()){
//
//                case CREATE:
//                    break;
//
//                default:
//                    log.error("Unhandled event type: {}", event.eventType());
//                    throw new InvalidKafkaOrderEventTypeException(event.eventType().name());
//            }
//        } catch (Exception e) {
//            log.error("Error processing Kafka message: {}", message, e);
//            throw new RuntimeException("Invalid order event payload", e);
//        }
//    }
}
