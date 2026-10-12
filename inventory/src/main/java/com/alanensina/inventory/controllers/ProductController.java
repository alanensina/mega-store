package com.alanensina.inventory.controllers;

import com.alanensina.inventory.dtos.CreateProductDTO;
import com.alanensina.inventory.dtos.ProductDTO;
import com.alanensina.inventory.dtos.UpdateProductDTO;
import com.alanensina.inventory.services.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDTO> create(@Valid @RequestBody CreateProductDTO productDTO) {
        return ResponseEntity.ok(productService.create(productDTO));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> createWithImage(
            @Valid @ModelAttribute CreateProductDTO productDTO,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(productService.create(productDTO, image));
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> list() {
        return ResponseEntity.ok(productService.listAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductByID(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductDTOById(id));
    }

    @PutMapping()
    public ResponseEntity<ProductDTO> updateProduct(@Valid @RequestBody UpdateProductDTO productDTO) {
        return ResponseEntity.ok(productService.updateProduct(productDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.deleteById(id));
    }

}
