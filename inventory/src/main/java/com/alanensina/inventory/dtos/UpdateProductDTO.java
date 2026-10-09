package com.alanensina.inventory.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductDTO(
        @NotNull(message = "ID is required") UUID id,
        @NotBlank(message = "Name is required") String name,
        boolean active,
        @DecimalMin(value = "0.00", message = "Price must be zero or greater") BigDecimal price,
        @Min(value = 0, message = "Quantity must be zero or greater") Integer quantity
) {
    public UpdateProductDTO(UUID id, String name, boolean active) {
        this(id, name, active, null, null);
    }
}
