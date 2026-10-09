package com.alanensina.notification.services;

import com.alanensina.notification.dtos.CartDTO;
import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.enums.NotificationType;
import com.alanensina.notification.enums.ReservationStatus;
import com.alanensina.notification.exceptions.EmailErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender sender;

    private EmailService emailService;
    private NotificationDTO event;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(sender);
        orderId = UUID.randomUUID();
        event = new NotificationDTO(
                "Alice",
                "alice@example.com",
                orderId,
                new CartDTO(
                        orderId,
                        1L,
                        UUID.randomUUID(),
                        new BigDecimal("19.99"),
                        ReservationStatus.PAID,
                        null,
                        null,
                        null),
                NotificationType.NEW_ORDER);
    }

    @Test
    void sendWelcomeEmailShouldSendExpectedMessage() {
        emailService.sendWelcomeEmail(event);

        SimpleMailMessage message = capturedMessage();
        assertArrayEquals(new String[]{"alice@example.com"}, message.getTo());
        assertEquals("New order placed: #1", message.getSubject());
        assertEquals("""
                Hello Alice
                Your order has been placed!

                Order number: 1
                Total price: 19.99€
                Thank you!""", message.getText());
    }

    @Test
    void sendCancellationEmailShouldSendExpectedMessage() {
        emailService.sendCancellationEmail(event);

        SimpleMailMessage message = capturedMessage();
        assertEquals("Order cancelled: #1", message.getSubject());
        assertEquals("""
                Hello Alice
                Your order has been cancelled!

                Order number: 1
                Thank you!""", message.getText());
    }

    @Test
    void sendWelcomeEmailShouldWrapSenderErrors() {
        doThrow(new IllegalStateException("mail server unavailable"))
                .when(sender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        EmailErrorException exception = assertThrows(
                EmailErrorException.class,
                () -> emailService.sendWelcomeEmail(event));

        assertEquals(
                "Error sending email: mail server unavailable",
                exception.getMessage());
    }

    private SimpleMailMessage capturedMessage() {
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        return captor.getValue();
    }
}
