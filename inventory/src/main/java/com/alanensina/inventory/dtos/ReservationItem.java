package com.alanensina.inventory.dtos;

import java.util.UUID;

public record ReservationItem(
        UUID productId,
        int quantity
) {
}
