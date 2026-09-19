package com.alanensina.order.services;

import com.alanensina.order.dtos.*;
import com.alanensina.order.enums.NotificationType;
import com.alanensina.order.enums.OrderEventType;
import com.alanensina.order.enums.ReservationStatus;
import com.alanensina.order.exceptions.CartNotFoundException;
import com.alanensina.order.models.Cart;
import com.alanensina.order.models.Item;
import com.alanensina.order.producers.OrderProducer;
import com.alanensina.order.repositories.CartRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.alanensina.order.exceptions.CustomerDetailsFetchException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
    private final OrderProducer orderProducer;
    private static final String CUSTOMER_API_URL = "http://localhost:8081";

    @Transactional
    public CartDTO create(@Valid CreateCartDTO cartDTO) {
        List<Item> items = new ArrayList<>();
        var totalPrice = BigDecimal.ZERO;
        var now = LocalDateTime.now(ZoneId.systemDefault());
        var customer = getCustomerDetails(cartDTO.customerId());

        var cart = Cart.builder()
                .customerId(cartDTO.customerId())
                .items(items)
                .createdAt(now)
                .updatedAt(now)
                .status(ReservationStatus.AWAITING_PAYMENT)
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

        orderProducer.sendOrderMessage(createOrderEventDTO(cart, OrderEventType.CREATE));
        orderProducer.sendNotificationMessage(createOrderNotificationEventDTO(cart, cart.getId(), customer, NotificationType.NEW_ORDER));

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

    @Transactional
    public CartDTO update(@Valid UpdateCartDTO cartDTO) {
        var existingCart = getOrderDTOById(cartDTO.id());

        List<Item> newItems = new ArrayList<>();
        var newTotalPrice = BigDecimal.ZERO;

        var updatedCart = Cart.builder()
                .id(cartDTO.id())
                .customerId(cartDTO.customerId())
                .createdAt(existingCart.createdAt())
                .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
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

        orderProducer.sendOrderMessage(createOrderEventDTO(updatedCart, OrderEventType.UPDATE));

        return toDTO(updatedCart);
    }

    @Transactional
    public CartDTO cancel(UUID id) {
        var cart = cartRepository.findById(id)
                .orElseThrow(() -> new CartNotFoundException(id));
        cart.setStatus(ReservationStatus.CANCELLED);
        var customer = getCustomerDetails(cart.getCustomerId());

        cartRepository.save(cart);
        orderProducer.sendOrderMessage(createOrderEventDTO(cart, OrderEventType.CANCEL));
        orderProducer.sendNotificationMessage(createOrderNotificationEventDTO(cart, id, customer, NotificationType.CANCEL));

        log.info("Cart cancelled with ID: {}", id);
        return toDTO(cart);
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

    private OrderEventDTO createOrderEventDTO(Cart cart, OrderEventType type) {
        return new OrderEventDTO(
                type,
                cart.getId(),
                cart.getItems().stream().map(item -> new ReservationItem(
                        item.getProductId(),
                        item.getQuantity())
                ).toList(),
                cart.getStatus(),
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    private CustomerDTO getCustomerDetails(@NotNull(message = "Customer ID is required") UUID uuid) {
        try {
            var requestAttributes = RequestContextHolder.getRequestAttributes();
            var authorization = requestAttributes instanceof ServletRequestAttributes servletAttributes
                    ? servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION)
                    : null;

            return RestClient.create(CUSTOMER_API_URL)
                    .get()
                    .uri("/api/customer/{id}", uuid)
                    .headers(headers -> {
                        if (authorization != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .retrieve()
                    .body(CustomerDTO.class);
        } catch (RestClientException ex) {
            throw new CustomerDetailsFetchException(uuid, ex);
        }
    }

    private NotificationDTO createOrderNotificationEventDTO(Cart cart, UUID orderId, CustomerDTO customer, NotificationType type) {
        return new NotificationDTO(
                customer.name(),
                customer.email(),
                orderId,
                new CartDTO(
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
                        ),
                type
        );
    }
}
