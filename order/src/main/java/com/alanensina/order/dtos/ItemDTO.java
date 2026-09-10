package com.alanensina.order.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDTO(
       @NotNull(message = "Product ID is required") UUID productId,
       @Min(value = 1, message = "Quantity must be more than zero") int quantity,
       @NotNull(message = "Price is required") BigDecimal price
) {
}
