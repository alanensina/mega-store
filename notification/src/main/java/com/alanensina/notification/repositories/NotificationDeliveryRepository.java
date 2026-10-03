package com.alanensina.notification.repositories;

import com.alanensina.notification.models.NotificationDelivery;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO NOTIFICATION_DELIVERY (order_id, notification_type, delivered_at)
            VALUES (:orderId, :notificationType, CURRENT_TIMESTAMP)
            ON CONFLICT (order_id, notification_type) DO NOTHING
            """, nativeQuery = true)
    int claimDelivery(
            @Param("orderId") UUID orderId,
            @Param("notificationType") String notificationType);

    @Modifying
    @Query(value = """
            UPDATE NOTIFICATION_DELIVERY
            SET delivered_at = CURRENT_TIMESTAMP
            WHERE order_id = :orderId
              AND notification_type = :notificationType
            """, nativeQuery = true)
    int markDelivered(
            @Param("orderId") UUID orderId,
            @Param("notificationType") String notificationType);
}
