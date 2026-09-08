package com.alanensina.inventory.dtos;

import java.util.UUID;

public record CreateStockDTO(
        UUID productId,
        int quantity
) {
}
