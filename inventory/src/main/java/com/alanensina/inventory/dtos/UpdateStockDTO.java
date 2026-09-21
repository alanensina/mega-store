package com.alanensina.inventory.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateStockDTO(
        @NotNull UUID id,
        @NotNull UUID productId,
        @Min(1) Integer quantity
) {
}
