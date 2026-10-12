package com.alanensina.inventory.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        BigDecimal price,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
