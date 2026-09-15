package com.alanensina.order.dtos;

import com.alanensina.order.enums.NotificationType;

import java.util.UUID;

public record NotificationDTO(
        String name,
        String email,
        UUID orderId,
        CartDTO cart,
        NotificationType notificationType
) {
}
