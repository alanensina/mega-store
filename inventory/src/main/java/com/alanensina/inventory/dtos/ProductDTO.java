package com.alanensina.inventory.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
