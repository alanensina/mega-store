package com.alanensina.order.dtos;

import com.alanensina.order.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartDTO(
        UUID id,
        UUID customerId,
        BigDecimal totalPrice,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ItemDTO> items
) {
}
