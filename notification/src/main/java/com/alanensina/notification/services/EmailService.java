package com.alanensina.notification.services;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.exceptions.EmailErrorException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender sender;

    @Value(value = "${spring.mail.username}")
    private String senderName;

    public EmailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void sendWelcomeEmail(NotificationDTO event) {
        sendEmail(
                event,
                getNewOrderSubject(event),
                getNewOrderTextMessage(event)
        );
    }

    public void sendCancellationEmail(NotificationDTO event) {
        sendEmail(
                event,
                getCancellationSubject(event),
                getCancellationText(event)
        );
    }

    private void sendEmail(NotificationDTO event, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.email());
            message.setSubject(subject);
            message.setText(text);
            sender.send(message);
        } catch (Exception e) {
            log.error("Error sending email: " + e.getMessage());
            throw new EmailErrorException(e.getMessage());
        }
    }

    private static @NonNull String getNewOrderSubject(NotificationDTO event) {
        return "New order placed: ID" + event.cart().id();
    }

    private static @NonNull String getCancellationSubject(NotificationDTO event) {
        return "Order cancelled: ID" + event.cart().id();
    }

    private String getNewOrderTextMessage(NotificationDTO event) {
        String header = """
                Hello %s
                Your order has been placed!
                """.formatted(event.name());

        StringBuilder message = new StringBuilder(header);
        message.append("\n");
        message.append("Order ID: ").append(event.cart().id()).append("\n");
        message.append("Total price: ").append(event.cart().totalPrice()).append("€").append("\n");
        message.append("Thank you!");
        return message.toString();
    }

    private String getCancellationText(NotificationDTO event) {
        String header = """
                Hello %s
                Your order has been cancelled!
                """.formatted(event.name());

        StringBuilder message = new StringBuilder(header);
        message.append("\n");
        message.append("Order ID: ").append(event.cart().id()).append("\n");
        message.append("Thank you!");
        return message.toString();
    }
}
