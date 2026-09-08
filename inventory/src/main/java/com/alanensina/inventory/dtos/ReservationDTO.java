package com.alanensina.inventory.dtos;

import com.alanensina.inventory.enums.ReservationStatus;

import java.util.List;
import java.util.UUID;

public record ReservationDTO(
        UUID id,
        UUID orderId,
        List<ReservationItemDTO> items,
        ReservationStatus status
) {
}
