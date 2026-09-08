package com.alanensina.inventory.controllers;

import com.alanensina.inventory.dtos.CreateProductDTO;
import com.alanensina.inventory.dtos.ProductDTO;
import com.alanensina.inventory.dtos.UpdateProductDTO;
import com.alanensina.inventory.services.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDTO> create(@Valid @RequestBody CreateProductDTO productDTO) {
        return ResponseEntity.ok(productService.create(productDTO));
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> list() {
        return ResponseEntity.ok(productService.listAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductByID(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductDTOById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable UUID id, @Valid @RequestBody UpdateProductDTO productDTO) {
        return ResponseEntity.ok(productService.updateProduct(id, productDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.deleteById(id));
    }

}
