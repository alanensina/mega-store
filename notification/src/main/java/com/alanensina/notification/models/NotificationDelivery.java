package com.alanensina.notification.models;

import com.alanensina.notification.enums.DeliveryStatus;
import com.alanensina.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
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
    private NotificationType notificationType;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public void setLeaseUntil(LocalDateTime leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

}
