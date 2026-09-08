package com.alanensina.inventory.dtos;

import java.util.UUID;

public record ReservationItemDTO(
        UUID productId,
        int quantity
) {
}
