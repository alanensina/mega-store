package com.alanensina.inventory.dtos;

import com.alanensina.inventory.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateReservationDTO(
        UUID cartId,
        List<ReservationItemDTO> items,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
