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
import static org.mockito.ArgumentMatchers.any;
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
                        1L,
                        UUID.randomUUID(),
                        BigDecimal.TEN,
                        ReservationStatus.PAID,
                        null,
                        null,
                        null),
                NotificationType.NEW_ORDER);
    }

    private static final int LEASE = NotificationDeliveryService.LEASE_SECONDS;

    @Test
    void deliverShouldIgnoreAlreadyDeliveredNotification() {
        when(repository.acquireLease(orderId, "NEW_ORDER", LEASE)).thenReturn(0);
        when(repository.isDelivered(orderId, "NEW_ORDER")).thenReturn(true);

        deliveryService.deliver(event);

        verifyNoInteractions(emailService);
        verify(repository, never()).markDelivered(any(), any());
    }

    @Test
    void deliverShouldFailWhenAnotherConsumerHoldsTheLease() {
        when(repository.acquireLease(orderId, "NEW_ORDER", LEASE)).thenReturn(0);
        when(repository.isDelivered(orderId, "NEW_ORDER")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> deliveryService.deliver(event));

        verifyNoInteractions(emailService);
        verify(repository, never()).markDelivered(any(), any());
    }

    @Test
    void deliverShouldSendWelcomeEmailAndPersistDelivery() {
        when(repository.acquireLease(orderId, "NEW_ORDER", LEASE)).thenReturn(1);

        deliveryService.deliver(event);

        var inOrder = inOrder(repository, emailService);
        inOrder.verify(repository).registerIfAbsent(orderId, "NEW_ORDER");
        inOrder.verify(repository).acquireLease(orderId, "NEW_ORDER", LEASE);
        inOrder.verify(emailService).sendWelcomeEmail(event);
        inOrder.verify(repository).markDelivered(orderId, "NEW_ORDER");
    }

    @Test
    void deliverShouldSendCancellationEmailAndPersistDelivery() {
        NotificationDTO cancellation = new NotificationDTO(
                event.name(),
                event.email(),
                event.orderId(),
                event.cart(),
                NotificationType.CANCEL);
        when(repository.acquireLease(orderId, "CANCEL", LEASE)).thenReturn(1);

        deliveryService.deliver(cancellation);

        var inOrder = inOrder(repository, emailService);
        inOrder.verify(repository).acquireLease(orderId, "CANCEL", LEASE);
        inOrder.verify(emailService).sendCancellationEmail(cancellation);
        inOrder.verify(repository).markDelivered(orderId, "CANCEL");
    }

    @Test
    void deliverShouldReleaseLeaseAndRethrowWhenEmailFails() {
        when(repository.acquireLease(orderId, "NEW_ORDER", LEASE)).thenReturn(1);
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService).sendWelcomeEmail(event);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deliveryService.deliver(event));

        assertEquals("SMTP unavailable", exception.getMessage());
        verify(repository).releaseLease(orderId, "NEW_ORDER");
        verify(repository, never()).markDelivered(any(), any());
    }
}
