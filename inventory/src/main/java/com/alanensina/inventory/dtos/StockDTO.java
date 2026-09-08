package com.alanensina.inventory.dtos;

import com.alanensina.inventory.models.Product;

import java.util.UUID;

public record StockDTO(
        UUID id,
        Product product,
        int quantity
) {
}
