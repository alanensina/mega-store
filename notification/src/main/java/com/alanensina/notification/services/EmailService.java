package com.alanensina.notification.services;

import com.alanensina.notification.dtos.NotificationDTO;
import com.alanensina.notification.exceptions.EmailErrorException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender sender;

    @Value(value = "${spring.mail.username}")
    private String senderName;

    public EmailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void sendWelcomeEmail(NotificationDTO event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.email());
            message.setSubject("New order placed: ID" + event.orderId());
            message.setText(getNewOrderTextMessage(event));
            sender.send(message);
        } catch (Exception e) {
            log.error("Error sending email: " + e.getMessage());
            throw new EmailErrorException(e.getMessage());
        }
    }

    public void sendCancellationEmail(NotificationDTO event) {
    }

    private String getNewOrderTextMessage(NotificationDTO dto) {

        String header = """
                Hello %s
                
                Your order has been placed!
                Check the list of items below:
                
                """.formatted(dto.name());

        StringBuilder message = new StringBuilder(header);
        message.append("\n");
        message.append("Total price: ").append(dto.cart().totalPrice()).append("€").append("\n");
        message.append("Thank you!");

        return message.toString();
    }



}
