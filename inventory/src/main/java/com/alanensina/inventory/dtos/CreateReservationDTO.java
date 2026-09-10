package com.alanensina.inventory.dtos;

import java.util.List;
import java.util.UUID;

public record CreateReservationDTO(
        UUID cartId,
        List<ReservationItemDTO> items
) {
}
