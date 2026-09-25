package com.alanensina.notification.services;

import com.alanensina.notification.dtos.CartDTO;
import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.enums.NotificationType;
import com.alanensina.notification.enums.ReservationStatus;
import com.alanensina.notification.exceptions.KafkaDataIntegrityViolationException;
import com.alanensina.notification.models.NotificationDelivery;
import com.alanensina.notification.repositories.NotificationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

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
        when(repository.existsByOrderIdAndNotificationType(orderId, NotificationType.NEW_ORDER))
                .thenReturn(true);

        deliveryService.deliver(event);

        verifyNoInteractions(emailService);
        verify(repository, never()).saveAndFlush(any(NotificationDelivery.class));
    }

    @Test
    void deliverShouldSendWelcomeEmailAndPersistDelivery() {
        when(repository.existsByOrderIdAndNotificationType(orderId, NotificationType.NEW_ORDER))
                .thenReturn(false);

        deliveryService.deliver(event);

        verify(emailService).sendWelcomeEmail(event);
        verifyPersistedDelivery();
    }

    @Test
    void deliverShouldSendCancellationEmailAndPersistDelivery() {
        NotificationDTO cancellation = new NotificationDTO(
                event.name(),
                event.email(),
                event.orderId(),
                event.cart(),
                NotificationType.CANCEL);
        when(repository.existsByOrderIdAndNotificationType(orderId, NotificationType.CANCEL))
                .thenReturn(false);

        deliveryService.deliver(cancellation);

        verify(emailService).sendCancellationEmail(cancellation);
        verifyPersistedDelivery();
    }

    @Test
    void deliverShouldWrapPersistenceIntegrityErrors() {
        when(repository.existsByOrderIdAndNotificationType(orderId, NotificationType.NEW_ORDER))
                .thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate delivery"))
                .when(repository).saveAndFlush(any(NotificationDelivery.class));

        KafkaDataIntegrityViolationException exception = assertThrows(
                KafkaDataIntegrityViolationException.class,
                () -> deliveryService.deliver(event));

        assertEquals("Data Integrity violation: duplicate delivery", exception.getMessage());
        verify(emailService).sendWelcomeEmail(event);
    }

    private void verifyPersistedDelivery() {
        ArgumentCaptor<NotificationDelivery> captor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(repository).saveAndFlush(captor.capture());
        assertNotNull(captor.getValue());
    }
}
