package com.alanensina.order.controllers;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.dtos.CreateCartDTO;
import com.alanensina.order.dtos.UpdateCartDTO;
import com.alanensina.order.services.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartControllerTest {

    private final CartService cartService = mock(CartService.class);
    private final CartController controller = new CartController(cartService);

    @Test
    void createsCart() {
        CreateCartDTO request = new CreateCartDTO(UUID.randomUUID(), List.of());
        CartDTO cart = mock(CartDTO.class);
        when(cartService.create(request)).thenReturn(cart);

        ResponseEntity<CartDTO> response = controller.create(request);

        assertSame(cart, response.getBody());
        verify(cartService).create(request);
    }

    @Test
    void listsCarts() {
        List<CartDTO> carts = List.of(mock(CartDTO.class));
        when(cartService.list()).thenReturn(carts);

        ResponseEntity<List<CartDTO>> response = controller.getAll();

        assertSame(carts, response.getBody());
        verify(cartService).list();
    }

    @Test
    void retrievesCartById() {
        UUID id = UUID.randomUUID();
        CartDTO cart = mock(CartDTO.class);
        when(cartService.getOrderDTOById(id)).thenReturn(cart);

        ResponseEntity<CartDTO> response = controller.getById(id);

        assertSame(cart, response.getBody());
        verify(cartService).getOrderDTOById(id);
    }

    @Test
    void updatesCart() {
        UpdateCartDTO request = new UpdateCartDTO(UUID.randomUUID(), UUID.randomUUID(), null, List.of());
        CartDTO cart = mock(CartDTO.class);
        when(cartService.update(request)).thenReturn(cart);

        ResponseEntity<CartDTO> response = controller.update(request);

        assertSame(cart, response.getBody());
        verify(cartService).update(request);
    }

    @Test
    void cancelsCart() {
        UUID id = UUID.randomUUID();
        CartDTO cart = mock(CartDTO.class);
        when(cartService.cancel(id)).thenReturn(cart);

        ResponseEntity<CartDTO> response = controller.cancel(id);

        assertSame(cart, response.getBody());
        verify(cartService).cancel(id);
    }
}
