package com.alanensina.order.dtos;

import java.util.UUID;

public record ReservationItem(
        UUID productId,
        int quantity
) {
}
