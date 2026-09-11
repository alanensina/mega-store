package com.alanensina.inventory.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record UpdateProductDTO(
        @NotNull(message = "ID is required") UUID id,
        @NotBlank(message = "Name is required") String name,
        boolean active
) {
}
