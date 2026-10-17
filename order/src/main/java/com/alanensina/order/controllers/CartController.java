package com.alanensina.order.controllers;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.dtos.CreateCartDTO;
import com.alanensina.order.dtos.UpdateCartDTO;
import com.alanensina.order.services.CartService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@AllArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartDTO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateCartDTO cartDTO
    ) {
        return ResponseEntity.ok(cartService.create(cartDTO, idempotencyKey));
    }

    @GetMapping
    public ResponseEntity<List<CartDTO>> getAll() {
        return ResponseEntity.ok(cartService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cartService.getOrderDTOById(id));
    }

    @PutMapping
    public ResponseEntity<CartDTO> update(@Valid @RequestBody UpdateCartDTO cartDTO) {
        return ResponseEntity.ok(cartService.update(cartDTO));
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<CartDTO> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(cartService.cancel(id));
    }
}
