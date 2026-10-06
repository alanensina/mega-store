package com.alanensina.notification.services;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.enums.NotificationType;
import com.alanensina.notification.repositories.NotificationDeliveryRepository;
import org.springframework.stereotype.Service;

/**
 * Sends each notification at least once and avoids duplicates in every case except a crash between
 * the SMTP call and {@code markDelivered}, where the lease expires and the e-mail is sent again.
 * The SMTP call is deliberately outside any database transaction.
 */
@Service
public class NotificationDeliveryService {

    static final int LEASE_SECONDS = 120;

    private final NotificationDeliveryRepository repository;
    private final EmailService emailService;

    public NotificationDeliveryService(
            NotificationDeliveryRepository repository,
            EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public void deliver(NotificationDTO event) {
        var orderId = event.orderId();
        var notificationType = event.notificationType().name();

        repository.registerIfAbsent(orderId, notificationType);
        if (repository.acquireLease(orderId, notificationType, LEASE_SECONDS) == 0) {
            if (repository.isDelivered(orderId, notificationType)) {
                return;
            }
            // Another consumer holds the lease; fail so the Kafka back-off retries after it finishes or expires.
            throw new IllegalStateException(
                    "Notification " + notificationType + " for order " + orderId + " is being delivered");
        }

        try {
            if (event.notificationType() == NotificationType.NEW_ORDER) {
                emailService.sendWelcomeEmail(event);
            } else {
                emailService.sendCancellationEmail(event);
            }
        } catch (RuntimeException exception) {
            repository.releaseLease(orderId, notificationType);
            throw exception;
        }

        repository.markDelivered(orderId, notificationType);
    }
}
