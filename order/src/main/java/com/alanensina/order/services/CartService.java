package com.alanensina.order.services;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.dtos.CreateCartDTO;
import com.alanensina.order.dtos.ItemDTO;
import com.alanensina.order.dtos.UpdateCartDTO;
import com.alanensina.order.enums.OrderStatus;
import com.alanensina.order.exceptions.CartNotFoundException;
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
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;

    public CartDTO create(@Valid CreateCartDTO cartDTO) {
        List<Item> items = new ArrayList<>();
        var totalPrice = BigDecimal.ZERO;
        var now = LocalDateTime.now(ZoneId.systemDefault());

        var cart = Cart.builder()
                .customerId(cartDTO.customerId())
                .items(items)
                .createdAt(now)
                .updatedAt(now)
                .status(OrderStatus.AWAITING_PAYMENT)
                .build();

        for (ItemDTO itemDTO : cartDTO.items()) {

            var item = Item.builder()
                    .productId(itemDTO.productId())
                    .quantity(itemDTO.quantity())
                    .price(itemDTO.price())
                    .cart(cart)
                    .build();
            items.add(item);
            totalPrice = calculateTotalPricePerItem(itemDTO, totalPrice);
        }

        cart.setTotalPrice(totalPrice);
        cart = cartRepository.save(cart);
        log.info("Cart created with ID: {}", cart.getId());

        // TODO: Send order to inventory

        return toDTO(cart);
    }

    public List<CartDTO> list() {
        return cartRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public CartDTO getOrderDTOById(UUID id) {
        var order = cartRepository.findById(id)
                .orElseThrow(() -> new CartNotFoundException(id));
        return toDTO(order);
    }

    public CartDTO update(@Valid UpdateCartDTO cartDTO) {
        var existingCart = getOrderDTOById(cartDTO.id());

        List<Item> newItems = new ArrayList<>();
        var newTotalPrice = BigDecimal.ZERO;

        var updatedCart = Cart.builder()
                .id(cartDTO.id())
                .customerId(cartDTO.customerId())
                .createdAt(existingCart.createdAt())
                .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
                .status(cartDTO.status())
                .items(newItems)
                .build();

        for (ItemDTO itemDTO : cartDTO.items()) {

            var item = Item.builder()
                    .productId(itemDTO.productId())
                    .quantity(itemDTO.quantity())
                    .price(itemDTO.price())
                    .cart(updatedCart)
                    .build();
            newItems.add(item);
            newTotalPrice = calculateTotalPricePerItem(itemDTO, newTotalPrice);
        }

        updatedCart.setTotalPrice(newTotalPrice);

        cartRepository.save(updatedCart);
        log.info("Cart updated with ID: {}", updatedCart.getId());

        // TODO: Send order to inventory

        return toDTO(updatedCart);
    }

    public CartDTO delete(UUID id) {
        var dto = getOrderDTOById(id);
        cartRepository.deleteById(id);
        log.info("Cart deleted with ID: {}", id);
        return dto;
    }

    private static @NonNull BigDecimal calculateTotalPricePerItem(ItemDTO itemDTO, BigDecimal totalPrice) {
        return totalPrice.add(itemDTO.price().multiply(BigDecimal.valueOf(itemDTO.quantity())));
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
                        item.getPrice())
                ).toList()
        );
    }
}
