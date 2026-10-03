package com.alanensina.notification.services;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.repositories.NotificationDeliveryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {

    private final NotificationDeliveryRepository repository;
    private final EmailService emailService;

    public NotificationDeliveryService(
            NotificationDeliveryRepository repository,
            EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Transactional
    public void deliver(NotificationDTO event) {
        var notificationType = event.notificationType().name();
        if (repository.claimDelivery(event.orderId(), notificationType) == 0) {
            return;
        }
        if (event.notificationType() == com.alanensina.notification.enums.NotificationType.NEW_ORDER) {
            emailService.sendWelcomeEmail(event);
        }
        else {
            emailService.sendCancellationEmail(event);
        }

        repository.markDelivered(event.orderId(), notificationType);
    }
}
