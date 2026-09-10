package com.alanensina.order.controllers;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.dtos.CreateCartDTO;
import com.alanensina.order.services.CartService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@AllArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartDTO> create(@Valid @RequestBody CreateCartDTO cartDTO) {
        return ResponseEntity.ok(cartService.create(cartDTO));
    }


}
