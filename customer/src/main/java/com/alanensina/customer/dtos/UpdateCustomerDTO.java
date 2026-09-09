package com.alanensina.customer.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCustomerDTO(
        @NotNull(message = "Id is mandatory") UUID id,
        @NotBlank(message = "Name is mandatory") String name,
        @Email(message = "Email is mandatory") String email,
        @NotBlank(message = "Username is mandatory") String username,
        @NotBlank(message = "Password is mandatory") String password
) {
}
