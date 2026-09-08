package com.alanensina.inventory.dtos;

import java.util.List;
import java.util.UUID;

public record CreateReservationDTO(
        UUID orderId,
        List<ReservationItemDTO> items
) {
}
