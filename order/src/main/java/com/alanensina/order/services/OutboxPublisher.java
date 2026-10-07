package com.alanensina.order.services;

import com.alanensina.order.models.OutboxEvent;
import com.alanensina.order.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Events that reach this many failed attempts stay in the table (published_at NULL) for manual inspection.
    @Value("${order.outbox.max-attempts:10}")
    private int maxAttempts = 10;

    @Value("${order.outbox.retry-initial-delay-ms:1000}")
    private long retryInitialDelayMs = 1_000L;

    @Value("${order.outbox.retry-max-delay-ms:300000}")
    private long retryMaxDelayMs = 300_000L;

    @Scheduled(fixedDelayString = "${order.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        for (OutboxEvent event : repository.findPendingEventsForPublishing(now, maxAttempts)) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                event.setPublishedAt(LocalDateTime.now(ZoneId.systemDefault()));
                event.setNextAttemptAt(null);
                event.setLastError(null);
                repository.save(event);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                recordFailure(event, exception);
                log.error("Interrupted while publishing outbox event {}", event.getId(), exception);
                return;
            } catch (Exception exception) {
                recordFailure(event, exception);
                log.error("Failed to publish outbox event {}", event.getId(), exception);
            }
        }
    }

    private void recordFailure(OutboxEvent event, Exception exception) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(exception.getMessage());
        if (attempts >= maxAttempts) {
            event.setNextAttemptAt(null);
            log.error("Outbox event {} gave up after {} attempts", event.getId(), attempts);
        } else {
            event.setNextAttemptAt(LocalDateTime.now(ZoneId.systemDefault())
                    .plus(backoffDelayMs(attempts), java.time.temporal.ChronoUnit.MILLIS));
        }
        repository.save(event);
    }

    private long backoffDelayMs(int attempts) {
        int shift = Math.min(attempts - 1, 20);
        return Math.min(retryInitialDelayMs << shift, retryMaxDelayMs);
    }
}
