package com.alanensina.notification.services;

import com.alanensina.notification.dtos.CartDTO;
import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.enums.NotificationType;
import com.alanensina.notification.enums.ReservationStatus;
import com.alanensina.notification.repositories.NotificationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private NotificationDeliveryRepository repository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationDeliveryService deliveryService;

    private UUID orderId;
    private NotificationDTO event;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        event = new NotificationDTO(
                "Alice",
                "alice@example.com",
                orderId,
                new CartDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.TEN,
                        ReservationStatus.PAID,
                        null,
                        null,
                        null),
                NotificationType.NEW_ORDER);
    }

    @Test
    void deliverShouldIgnoreAlreadyDeliveredNotification() {
        when(repository.claimDelivery(orderId, NotificationType.NEW_ORDER.name())).thenReturn(0);

        deliveryService.deliver(event);

        verifyNoInteractions(emailService);
        verify(repository).claimDelivery(orderId, NotificationType.NEW_ORDER.name());
    }

    @Test
    void deliverShouldSendWelcomeEmailAndPersistDelivery() {
        when(repository.claimDelivery(orderId, NotificationType.NEW_ORDER.name())).thenReturn(1);

        deliveryService.deliver(event);

        var inOrder = inOrder(repository, emailService);
        inOrder.verify(repository).claimDelivery(orderId, NotificationType.NEW_ORDER.name());
        inOrder.verify(emailService).sendWelcomeEmail(event);
        inOrder.verify(repository).markDelivered(orderId, NotificationType.NEW_ORDER.name());
    }

    @Test
    void deliverShouldSendCancellationEmailAndPersistDelivery() {
        NotificationDTO cancellation = new NotificationDTO(
                event.name(),
                event.email(),
                event.orderId(),
                event.cart(),
                NotificationType.CANCEL);
        when(repository.claimDelivery(orderId, NotificationType.CANCEL.name())).thenReturn(1);

        deliveryService.deliver(cancellation);

        var inOrder = inOrder(repository, emailService);
        inOrder.verify(repository).claimDelivery(orderId, NotificationType.CANCEL.name());
        inOrder.verify(emailService).sendCancellationEmail(cancellation);
        inOrder.verify(repository).markDelivered(orderId, NotificationType.CANCEL.name());
    }

    @Test
    void deliverShouldNotMarkDeliveryWhenEmailFails() {
        when(repository.claimDelivery(orderId, NotificationType.NEW_ORDER.name())).thenReturn(1);
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService).sendWelcomeEmail(event);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deliveryService.deliver(event));

        assertEquals("SMTP unavailable", exception.getMessage());
        verify(repository).claimDelivery(orderId, NotificationType.NEW_ORDER.name());
        verify(repository, never())
                .markDelivered(orderId, NotificationType.NEW_ORDER.name());
    }
}
