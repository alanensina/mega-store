package com.alanensina.inventory.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStockDTO(
        @NotNull(message = "Product ID is mandatory") UUID productId,
        @Min(1) int quantity
) {
}
