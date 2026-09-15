package com.alanensina.notification.dtos;

import com.alanensina.notification.enums.NotificationType;

import java.util.UUID;

public record NotificationDTO(
        String name,
        String email,
        UUID orderId,
        CartDTO cart,
        NotificationType notificationType
) {
}
