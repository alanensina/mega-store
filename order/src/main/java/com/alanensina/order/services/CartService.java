package com.alanensina.order.services;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.dtos.CreateCartDTO;
import com.alanensina.order.dtos.ItemDTO;
import com.alanensina.order.enums.OrderStatus;
import com.alanensina.order.models.Cart;
import com.alanensina.order.models.Item;
import com.alanensina.order.repositories.CartRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;

    public CartDTO create(@Valid CreateCartDTO cartDTO) {
        List<Item> items = new ArrayList<>();
        var totalPrice = BigDecimal.ZERO;

        for (ItemDTO itemDTO : cartDTO.items()) {

            var item = Item.builder()
                    .productId(itemDTO.productId())
                    .quantity(itemDTO.quantity())
                    .price(itemDTO.price())
                    .build();
            items.add(item);
            totalPrice = calculateTotalPricePerItem(itemDTO, totalPrice);
        }

        var now = LocalDateTime.now(ZoneId.systemDefault());

        var cart = Cart.builder()
                .customerId(cartDTO.customerId())
                .items(items)
                .totalPrice(totalPrice)
                .createdAt(now)
                .updatedAt(now)
                .status(OrderStatus.AWAITING_PAYMENT)
                .build();

        cart = cartRepository.save(cart);
        log.info("Cart created with ID: {}", cart.getId());

        return toDTO(cart);
    }

    private CartDTO toDTO(Cart cart) {
        return new CartDTO(
                cart.getId(),
                cart.getCustomerId(),
                cart.getTotalPrice(),
                cart.getStatus(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                cart.getItems().stream().map(item -> new ItemDTO(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()
                )).toList()
        );
    }

    private static @NonNull BigDecimal calculateTotalPricePerItem(ItemDTO itemDTO, BigDecimal totalPrice) {
        return totalPrice.add(itemDTO.price().multiply(BigDecimal.valueOf(itemDTO.quantity())));
    }
}
