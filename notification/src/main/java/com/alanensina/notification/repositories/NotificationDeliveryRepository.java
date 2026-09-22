package com.alanensina.notification.repositories;

import com.alanensina.notification.enums.NotificationType;
import com.alanensina.notification.models.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    boolean existsByOrderIdAndNotificationType(UUID orderId, NotificationType notificationType);
}
