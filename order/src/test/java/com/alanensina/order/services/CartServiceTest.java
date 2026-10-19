package com.alanensina.order.services;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.dtos.CreateCartDTO;
import com.alanensina.order.dtos.CustomerDTO;
import com.alanensina.order.dtos.ItemDTO;
import com.alanensina.order.dtos.NotificationDTO;
import com.alanensina.order.dtos.OrderEventDTO;
import com.alanensina.order.dtos.UpdateCartDTO;
import com.alanensina.order.enums.ReservationStatus;
import com.alanensina.order.exceptions.CartNotFoundException;
import com.alanensina.order.exceptions.CustomerDetailsFetchException;
import com.alanensina.order.models.Cart;
import com.alanensina.order.models.Item;
import com.alanensina.order.repositories.CartRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final OutboxService outboxService = mock(OutboxService.class);
    private final CustomerApiClient customerApiClient = mock(CustomerApiClient.class);
    private final CartIdempotencyService cartIdempotencyService = mock(CartIdempotencyService.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final CartService cartService = new CartService(
            cartRepository,
            outboxService,
            customerApiClient,
            cartIdempotencyService,
            transactionManager);

    private UUID cartId;
    private UUID customerId;
    private UUID productId;
    private CustomerDTO customer;
    private ItemDTO itemDTO;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        customer = new CustomerDTO(customerId, "Alice", "alice@example.com", "alice", null, null);
        itemDTO = new ItemDTO(productId, 2, new BigDecimal("12.50"));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createCalculatesTotalAndEmitsOrderAndNotificationEvents() {
        when(cartRepository.saveAndFlush(any(Cart.class))).thenAnswer(invocation -> {
            Cart savedCart = invocation.getArgument(0);
            savedCart.setId(cartId);
            return savedCart;
        });
        when(customerApiClient.fetchCustomer(customerId, "Bearer test-token")).thenReturn(customer);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        CartDTO result = cartService.create(new CreateCartDTO(customerId, List.of(itemDTO)));

        assertEquals(cartId, result.id());
        assertEquals(new BigDecimal("25.00"), result.totalPrice());
        assertEquals(ReservationStatus.AWAITING_PAYMENT, result.status());
        assertEquals(List.of(itemDTO), result.items());
        verify(customerApiClient).fetchCustomer(customerId, "Bearer test-token");
        verify(outboxService).addOrderEvent(any(OrderEventDTO.class));
        verify(outboxService).addNotificationEvent(any(NotificationDTO.class));
    }

    @Test
    void createWrapsCustomerLookupFailures() {
        when(customerApiClient.fetchCustomer(customerId, null))
                .thenThrow(new RestClientException("customer service unavailable"));

        CreateCartDTO createCartDTO = new CreateCartDTO(customerId, List.of(itemDTO));
        CustomerDetailsFetchException exception = assertThrows(
                CustomerDetailsFetchException.class,
                () -> cartService.create(createCartDTO));
        assertNotNull(exception.getCause());
    }

    @Test
    void createTreatsBlankIdempotencyKeyAsARegularRequest() {
        when(cartRepository.saveAndFlush(any(Cart.class))).thenAnswer(invocation -> {
            Cart savedCart = invocation.getArgument(0);
            savedCart.setId(cartId);
            return savedCart;
        });
        when(customerApiClient.fetchCustomer(customerId, null)).thenReturn(customer);

        CartDTO result = cartService.create(new CreateCartDTO(customerId, List.of(itemDTO)), "  ");

        assertEquals(cartId, result.id());
        verify(cartIdempotencyService, never()).executeOrReplay(any(), any(), any());
    }

    @Test
    void createUsesIdempotencyServiceForNonBlankKey() {
        when(cartRepository.saveAndFlush(any(Cart.class))).thenAnswer(invocation -> {
            Cart savedCart = invocation.getArgument(0);
            savedCart.setId(cartId);
            return savedCart;
        });
        when(customerApiClient.fetchCustomer(customerId, null)).thenReturn(customer);
        when(cartIdempotencyService.executeOrReplay(any(), any(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<CartDTO>) invocation.getArgument(1)).get());

        CartDTO result = cartService.create(new CreateCartDTO(customerId, List.of(itemDTO)), "request-key");

        assertEquals(cartId, result.id());
        verify(cartIdempotencyService).executeOrReplay(any(), any(), any());
        verify(customerApiClient).fetchCustomer(customerId, null);
    }

    @Test
    void listMapsRepositoryResultsToDtos() {
        when(cartRepository.findAll()).thenReturn(List.of(cartWithItem()));

        List<CartDTO> results = cartService.list();

        assertEquals(1, results.size());
        assertEquals(cartId, results.getFirst().id());
        assertEquals(List.of(itemDTO), results.getFirst().items());
    }

    @Test
    void getOrderDTOByIdReturnsCartAndThrowsWhenMissing() {
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cartWithItem()), Optional.empty());

        assertEquals(cartId, cartService.getOrderDTOById(cartId).id());
        assertThrows(CartNotFoundException.class, () -> cartService.getOrderDTOById(cartId));
    }

    @Test
    void updateReplacesItemsAndCalculatesNewTotal() {
        Cart cart = cartWithItem();
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        ItemDTO replacement = new ItemDTO(UUID.randomUUID(), 3, new BigDecimal("4.00"));

        CartDTO result = cartService.update(new UpdateCartDTO(
                cartId, customerId, ReservationStatus.PAID, List.of(replacement)));

        assertEquals(customerId, result.customerId());
        assertEquals(ReservationStatus.PAID, result.status());
        assertEquals(new BigDecimal("12.00"), result.totalPrice());
        assertEquals(List.of(replacement), result.items());
        assertNotNull(result.updatedAt());
        verify(outboxService).addOrderEvent(any(OrderEventDTO.class));
    }

    @Test
    void updateThrowsWhenCartDoesNotExist() {
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());
        UpdateCartDTO updateCartDTO = new UpdateCartDTO(
                cartId, customerId, ReservationStatus.PAID, List.of(itemDTO));

        assertThrows(CartNotFoundException.class, () -> cartService.update(updateCartDTO));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void cancelMarksCartAndEmitsCancellationEvents() {
        Cart cart = cartWithItem();
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart), Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(customerApiClient.fetchCustomer(customerId, null)).thenReturn(customer);

        CartDTO result = cartService.cancel(cartId);

        assertEquals(ReservationStatus.CANCELLED, result.status());
        assertNotNull(result.updatedAt());
        verify(cartRepository).save(cart);
        verify(outboxService).addOrderEvent(any(OrderEventDTO.class));
        verify(outboxService).addNotificationEvent(any(NotificationDTO.class));
    }

    @Test
    void cancelReturnsAlreadyCancelledCartWithoutPublishing() {
        Cart cart = cartWithItem();
        cart.setStatus(ReservationStatus.CANCELLED);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart), Optional.of(cart));
        when(customerApiClient.fetchCustomer(customerId, null)).thenReturn(customer);

        CartDTO result = cartService.cancel(cartId);

        assertEquals(ReservationStatus.CANCELLED, result.status());
        verify(cartRepository, never()).save(any(Cart.class));
        verify(outboxService, never()).addOrderEvent(any(OrderEventDTO.class));
        verify(outboxService, never()).addNotificationEvent(any(NotificationDTO.class));
    }

    @Test
    void cancelThrowsWhenCartIsMissingBeforeCustomerLookup() {
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> cartService.cancel(cartId));
    }

    @Test
    void cancelThrowsWhenCartDisappearsDuringTransaction() {
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cartWithItem()), Optional.empty());
        when(customerApiClient.fetchCustomer(customerId, null)).thenReturn(customer);

        assertThrows(CartNotFoundException.class, () -> cartService.cancel(cartId));
    }

    private Cart cartWithItem() {
        Cart cart = Cart.builder()
                .id(cartId)
                .customerId(customerId)
                .totalPrice(new BigDecimal("25.00"))
                .status(ReservationStatus.AWAITING_PAYMENT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        cart.getItems().add(Item.builder()
                .cart(cart)
                .productId(productId)
                .quantity(itemDTO.quantity())
                .price(itemDTO.price())
                .build());
        return cart;
    }
}
