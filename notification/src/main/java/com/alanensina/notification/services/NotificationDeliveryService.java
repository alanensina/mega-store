package com.alanensina.notification.services;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.exceptions.KafkaDataIntegrityViolationException;
import com.alanensina.notification.models.NotificationDelivery;
import com.alanensina.notification.repositories.NotificationDeliveryRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
        if (repository.existsByOrderIdAndNotificationType(event.orderId(), event.notificationType())) {
            return;
        }

        try {
            if (event.notificationType() == com.alanensina.notification.enums.NotificationType.NEW_ORDER) {
                emailService.sendWelcomeEmail(event);
            } else {
                emailService.sendCancellationEmail(event);
            }

            var delivery = new NotificationDelivery();
            delivery.setOrderId(event.orderId());
            delivery.setNotificationType(event.notificationType());
            delivery.setDeliveredAt(LocalDateTime.now(ZoneId.systemDefault()));
            repository.saveAndFlush(delivery);
        } catch (DataIntegrityViolationException e) {
            throw new KafkaDataIntegrityViolationException(e.getMessage());
        }
    }
}
