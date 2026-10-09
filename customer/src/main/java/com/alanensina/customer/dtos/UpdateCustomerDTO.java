package com.alanensina.customer.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCustomerDTO(
        @NotNull(message = "Id is mandatory") UUID id,
        @NotBlank(message = "Name is mandatory") String name,
        @Email(message = "Email is mandatory") String email,
        @NotBlank(message = "Username is mandatory") String username,
        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
        String password
) {
}
