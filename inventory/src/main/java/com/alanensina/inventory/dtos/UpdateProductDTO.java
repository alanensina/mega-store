package com.alanensina.inventory.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductDTO(
        @NotBlank(message = "Name is required") String name,
        boolean active
) {
}
