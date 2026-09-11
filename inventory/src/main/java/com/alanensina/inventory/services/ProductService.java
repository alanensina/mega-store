package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.CreateProductDTO;
import com.alanensina.inventory.dtos.ProductDTO;
import com.alanensina.inventory.dtos.UpdateProductDTO;
import com.alanensina.inventory.exceptions.ProductNameAlreadyExistsException;
import com.alanensina.inventory.exceptions.ProductNotFoundException;
import com.alanensina.inventory.models.Product;
import com.alanensina.inventory.repositories.ProductRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public ProductDTO create(CreateProductDTO productDTO) {

        validateProductName(productDTO.name());

        var now = LocalDateTime.now(ZoneId.systemDefault());
        var newProduct = Product.builder()
                .name(productDTO.name())
                .active(productDTO.active())
                .createdAt(now)
                .updatedAt(now)
                .build();

        newProduct =  productRepository.save(newProduct);
        log.info("Product with name '" + productDTO.name() + "' has been created");

        return toDTO(newProduct);
    }

    public List<ProductDTO> listAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductDTO getProductDTOById(UUID id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toDTO(product);
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ProductDTO updateProduct(@Valid UpdateProductDTO productDTO) {

        var product = getProductDTOById(productDTO.id());

        validateProductName(productDTO.name());

        var updatedProduct = Product.builder()
                .id(productDTO.id())
                .name(productDTO.name())
                .active(productDTO.active())
                .createdAt(product.createdAt())
                .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
                .build();

        productRepository.save(updatedProduct);

        log.info("Product with id '" + productDTO.id() + "' has been updated");
        return toDTO(updatedProduct);
    }

    public ProductDTO deleteById(UUID id) {
        var product = getProductDTOById(id);
        productRepository.deleteById(id);
        log.info("Product with id '" + id + "' has been deleted");
        return product;
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private void validateProductName(String name) {
        if (productRepository.existsByNameIgnoreCase(name)) {
            log.error("Product with name '" + name + "' already exists");
            throw new ProductNameAlreadyExistsException(name);
        }
    }
}
