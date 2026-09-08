package com.alanensina.inventory.dtos;

import java.util.UUID;

public record UpdateStockDTO(
        UUID id,
        UUID productId,
        Integer quantity
) {
}
