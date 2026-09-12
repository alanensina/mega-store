package com.alanensina.order.producers;

import com.alanensina.order.dtos.OrderEventDTO;
import com.alanensina.order.exceptions.KafkaOrderProducerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class OrderProducer {

    private final NewTopic topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendMessage(OrderEventDTO event){
        log.info("Sending Order event -> {}", event);

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic.name(), payload);
        } catch (Exception e) {
            log.error("Error to send an Order event -> {}", event, e);
            throw new KafkaOrderProducerException(e.getMessage());
        }

        log.info("Order event sent successfully! -> {}", event);
    }
}
