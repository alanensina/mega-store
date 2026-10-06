package com.alanensina.notification;

import com.alanensina.notification.dtos.CartDTO;
import com.alanensina.notification.dtos.ItemDTO;
import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.enums.NotificationType;
import com.alanensina.notification.enums.ReservationStatus;
import com.alanensina.notification.exceptions.EmailErrorException;
import com.alanensina.notification.exceptions.InvalidKafkaNotificationEventTypeException;
import com.alanensina.notification.exceptions.KafkaDataIntegrityViolationException;
import com.alanensina.notification.exceptions.KafkaErrorException;
import com.alanensina.notification.enums.DeliveryStatus;
import com.alanensina.notification.models.NotificationDelivery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationDomainTest {

    @Test
    void recordsAndEnumsShouldExposeTheirValues() {
        UUID id = UUID.randomUUID();
        ItemDTO item = new ItemDTO(id, 2, BigDecimal.TEN);
        LocalDateTime createdAt = LocalDateTime.now();
        CartDTO cart = new CartDTO(
                id,
                id,
                BigDecimal.TEN,
                ReservationStatus.PAID,
                createdAt,
                createdAt,
                List.of(item));
        NotificationDTO notification = new NotificationDTO(
                "Alice", "alice@example.com", id, cart, NotificationType.NEW_ORDER);

        assertEquals(id, item.productId());
        assertEquals(2, item.quantity());
        assertEquals(BigDecimal.TEN, item.price());
        assertEquals(id, cart.id());
        assertEquals(id, cart.customerId());
        assertEquals(BigDecimal.TEN, cart.totalPrice());
        assertEquals(ReservationStatus.PAID, cart.status());
        assertEquals(createdAt, cart.createdAt());
        assertEquals(createdAt, cart.updatedAt());
        assertIterableEquals(List.of(item), cart.items());
        assertEquals("Alice", notification.name());
        assertEquals("alice@example.com", notification.email());
        assertEquals(id, notification.orderId());
        assertEquals(cart, notification.cart());
        assertEquals(NotificationType.NEW_ORDER, notification.notificationType());
        assertEquals(2, NotificationType.values().length);
        assertEquals(5, ReservationStatus.values().length);
    }

    @Test
    void notificationDeliveryShouldSupportConstructorsAndSetters() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setOrderId(UUID.randomUUID());
        delivery.setNotificationType(NotificationType.NEW_ORDER);
        delivery.setDeliveredAt(LocalDateTime.now());
        assertNotNull(delivery);
        delivery.setOrderId(UUID.randomUUID());
        delivery.setNotificationType(NotificationType.CANCEL);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setStatus(DeliveryStatus.SENT);
        delivery.setLeaseUntil(LocalDateTime.now());
        assertEquals(3, DeliveryStatus.values().length);
    }

    @Test
    void exceptionsShouldDescribeTheirErrors() {
        assertEquals(
                "Error sending email: smtp unavailable",
                new EmailErrorException("smtp unavailable").getMessage());
        assertEquals(
                "Invalid notification event type: null",
                new InvalidKafkaNotificationEventTypeException("null").getMessage());
        assertEquals(
                "Data Integrity violation: duplicate",
                new KafkaDataIntegrityViolationException("duplicate").getMessage());
        assertEquals(
                "Error processing Kafka message: malformed",
                new KafkaErrorException("malformed").getMessage());
    }
}
