package com.alanensina.order.dtos;

import com.alanensina.order.enums.ReservationStatus;

import java.util.List;
import java.util.UUID;

public record UpdateCartDTO(
        UUID id,
        UUID customerId,
        ReservationStatus status,
        List<ItemDTO> items
) {
}
