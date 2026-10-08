package com.alanensina.inventory.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductDTO(
        @NotBlank(message = "Name is required") String name,
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must be zero or greater") BigDecimal price,
        boolean active,
        @Min(value = 0, message = "Quantity must be zero or greater") int quantity
) {
}
