package com.alanensina.inventory.dtos;

import java.util.UUID;

public record ReservationItemEvent(
        UUID productId,
        int quantity
) {
}
