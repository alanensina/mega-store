package com.alanensina.notification.repositories;

import com.alanensina.notification.models.NotificationDelivery;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Every modifying method commits on its own, so the e-mail is never sent inside a database transaction.
 */
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO NOTIFICATION_DELIVERY (order_id, notification_type, status)
            VALUES (:orderId, :notificationType, 'PENDING')
            ON CONFLICT (order_id, notification_type) DO NOTHING
            """, nativeQuery = true)
    int registerIfAbsent(
            @Param("orderId") UUID orderId,
            @Param("notificationType") String notificationType);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE NOTIFICATION_DELIVERY
            SET status = 'SENDING',
                lease_until = CURRENT_TIMESTAMP + make_interval(secs => :leaseSeconds)
            WHERE order_id = :orderId
              AND notification_type = :notificationType
              AND (status = 'PENDING'
                   OR (status = 'SENDING' AND lease_until < CURRENT_TIMESTAMP))
            """, nativeQuery = true)
    int acquireLease(
            @Param("orderId") UUID orderId,
            @Param("notificationType") String notificationType,
            @Param("leaseSeconds") int leaseSeconds);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE NOTIFICATION_DELIVERY
            SET status = 'SENT',
                delivered_at = CURRENT_TIMESTAMP,
                lease_until = NULL
            WHERE order_id = :orderId
              AND notification_type = :notificationType
            """, nativeQuery = true)
    int markDelivered(
            @Param("orderId") UUID orderId,
            @Param("notificationType") String notificationType);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE NOTIFICATION_DELIVERY
            SET status = 'PENDING',
                lease_until = NULL
            WHERE order_id = :orderId
              AND notification_type = :notificationType
              AND status = 'SENDING'
            """, nativeQuery = true)
    int releaseLease(
            @Param("orderId") UUID orderId,
            @Param("notificationType") String notificationType);

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM NOTIFICATION_DELIVERY
            WHERE order_id = :orderId
              AND notification_type = :notificationType
              AND status = 'SENT'
            """, nativeQuery = true)
    boolean isDelivered(
            @Param("orderId") UUID orderId,
            @Param("notificationType") String notificationType);
}
