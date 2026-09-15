package com.alanensina.order.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerDTO(
        UUID id,
        String name,
        String email,
        String username,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
