package com.alanensina.customer.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerDTO(
        @NotBlank(message = "Name is mandatory") String name,
        @Email(message = "Email is mandatory") String email,
        @NotBlank(message = "Username is mandatory") String username,
        @NotBlank(message = "Password is mandatory") String password
) {
}
