package com.alanensina.inventory.dtos;

import com.alanensina.inventory.enums.OrderEventType;
import com.alanensina.inventory.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderEventDTO(
        OrderEventType eventType,
        UUID cartId,
        List<ReservationItemDTO> items,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
