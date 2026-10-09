package com.alanensina.notification.dtos;

import com.alanensina.notification.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartDTO(
        UUID id,
        Long orderNumber,
        UUID customerId,
        BigDecimal totalPrice,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ItemDTO> items
) {
}
