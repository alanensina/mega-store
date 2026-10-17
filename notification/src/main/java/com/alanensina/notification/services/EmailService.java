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

    @Value("${notification.email.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${notification.email.retry.initial-delay-ms:1000}")
    private long retryInitialDelayMs;

    @Value("${notification.email.retry.max-delay-ms:5000}")
    private long retryMaxDelayMs;

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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.email());
        message.setSubject(subject);
        message.setText(text);
        message.setFrom(senderName);

        int totalAttempts = Math.max(1, maxAttempts);
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                sender.send(message);
                return;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt < totalAttempts) {
                    log.warn("Email send failed on attempt {}/{}. Retrying.", attempt, totalAttempts);
                    sleepBeforeRetry(attempt);
                }
            }
        }

        if (lastException != null) {
            log.error("Error sending email after {} attempts: {}", totalAttempts, lastException.getMessage());
            throw new EmailErrorException(lastException.getMessage());
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(backoffDelayMs(attempt));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EmailErrorException("Email retry interrupted.");
        }
    }

    private long backoffDelayMs(int attempt) {
        long initialDelay = Math.max(1L, retryInitialDelayMs);
        long maxDelay = Math.max(initialDelay, retryMaxDelayMs);
        long shift = Math.max(0, attempt - 1);
        long delay = initialDelay << Math.min(shift, 30);
        return Math.min(delay, maxDelay);
    }

    private static @NonNull String getNewOrderSubject(NotificationDTO event) {
        return "New order placed: #" + event.cart().orderNumber();
    }

    private static @NonNull String getCancellationSubject(NotificationDTO event) {
        return "Order cancelled: #" + event.cart().orderNumber();
    }

    private String getNewOrderTextMessage(NotificationDTO event) {
        String header = """
                Hello %s
                Your order has been placed!
                """.formatted(event.name());

        StringBuilder message = new StringBuilder(header);
        message.append("\n");
        message.append("Order number: ").append(event.cart().orderNumber()).append("\n");
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
        message.append("Order number: ").append(event.cart().orderNumber()).append("\n");
        message.append("Thank you!");
        return message.toString();
    }
}
