package com.alanensina.notification.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_DELIVERY",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_delivery_event",
                columnNames = {"order_id", "notification_type"}))
public class NotificationDelivery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private com.alanensina.notification.enums.NotificationType notificationType;

    @Column(name = "delivered_at", nullable = false)
    private LocalDateTime deliveredAt;

    public NotificationDelivery() {
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setNotificationType(com.alanensina.notification.enums.NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
