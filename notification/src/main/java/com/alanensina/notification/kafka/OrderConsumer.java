package com.alanensina.notification.kafka;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.exceptions.InvalidKafkaNotificationEventTypeException;
import com.alanensina.notification.exceptions.KafkaErrorException;
import com.alanensina.notification.services.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@AllArgsConstructor
@Slf4j
public class OrderConsumer {

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message){
        try {
            var event = objectMapper.readValue(message, NotificationDTO.class);

            switch(event.notificationType()){

                case NEW_ORDER:
                    emailService.sendWelcomeEmail(event);
                    break;

                case CANCEL:
                    emailService.sendCancellationEmail(event);
                    break;

                default:
                    log.error("Unhandled notification event type: {}", event.notificationType().name());
                    throw new InvalidKafkaNotificationEventTypeException(event.notificationType().name());
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
            throw new KafkaErrorException(e.getMessage());
        }
    }
}
