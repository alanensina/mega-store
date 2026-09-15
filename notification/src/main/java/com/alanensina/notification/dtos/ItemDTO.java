package com.alanensina.notification.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDTO(
        UUID productId,
        int quantity,
        BigDecimal price
) {
}
