package com.alanensina.order.dtos;

import com.alanensina.order.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

public record UpdateCartDTO(
        UUID id,
        UUID customerId,
        OrderStatus status,
        List<ItemDTO> items
) {
}
