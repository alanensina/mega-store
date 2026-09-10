package com.alanensina.order.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateCartDTO(
        @NotNull(message = "Customer ID is required") UUID customerId,
        @NotEmpty(message = "Items list cannot be empty") List<ItemDTO> items
) {
}
