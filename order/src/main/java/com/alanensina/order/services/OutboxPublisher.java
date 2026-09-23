package com.alanensina.order.services;

import com.alanensina.order.models.OutboxEvent;
import com.alanensina.order.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${order.outbox.publish-delay-ms:1000}")
    public void publishPendingEvents() {
        for (OutboxEvent event : repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                event.setPublishedAt(LocalDateTime.now(ZoneId.systemDefault()));
                event.setLastError(null);
                repository.save(event);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(exception.getMessage());
                repository.save(event);
                log.error("Interrupted while publishing outbox event {}", event.getId(), exception);
                return;
            } catch (Exception exception) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(exception.getMessage());
                repository.save(event);
                log.error("Failed to publish outbox event {}", event.getId(), exception);
            }
        }
    }
}
