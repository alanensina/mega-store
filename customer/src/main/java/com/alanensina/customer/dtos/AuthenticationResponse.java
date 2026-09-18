package com.alanensina.customer.dtos;

public record AuthenticationResponse(
        CustomerDTO customer,
        String token
) {
}
