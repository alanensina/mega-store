package com.alanensina.order.dtos;

import com.alanensina.order.enums.OrderEventType;
import com.alanensina.order.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderEventDTO(
        OrderEventType eventType,
        UUID cartId,
        List<ReservationItem> items,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID eventId
) {
}
