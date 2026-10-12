package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.CreateProductDTO;
import com.alanensina.inventory.dtos.ProductDTO;
import com.alanensina.inventory.dtos.UpdateProductDTO;
import com.alanensina.inventory.exceptions.ProductNameAlreadyExistsException;
import com.alanensina.inventory.exceptions.ProductNotFoundException;
import com.alanensina.inventory.models.Product;
import com.alanensina.inventory.models.Stock;
import com.alanensina.inventory.repositories.ProductRepository;
import com.alanensina.inventory.repositories.StockRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final ProductImageStorageService productImageStorageService;

    @Transactional
    public ProductDTO create(CreateProductDTO productDTO) {
        return create(productDTO, null);
    }

    @Transactional
    public ProductDTO create(CreateProductDTO productDTO, MultipartFile image) {

        validateProductName(productDTO.name());

        var now = LocalDateTime.now(ZoneId.systemDefault());
        var imageUrl = image != null && !image.isEmpty()
                ? productImageStorageService.uploadProductImage(image)
                : null;
        var newProduct = Product.builder()
                .name(productDTO.name())
                .price(productDTO.price())
                .imageUrl(imageUrl)
                .active(productDTO.active())
                .createdAt(now)
                .updatedAt(now)
                .build();

        newProduct = productRepository.save(newProduct);

        var stock = new Stock();
        stock.setProduct(newProduct);
        stock.setQuantity(productDTO.quantity());
        stock.setCreatedAt(now);
        stock.setUpdatedAt(now);
        stockRepository.save(stock);
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

    @Transactional
    public ProductDTO updateProduct(@Valid UpdateProductDTO productDTO) {

        var product = getProductDTOById(productDTO.id());

        if (!product.name().equalsIgnoreCase(productDTO.name())) {
            validateProductName(productDTO.name());
        }

        var updatedProduct = Product.builder()
                .id(productDTO.id())
                .name(productDTO.name())
                .price(productDTO.price() != null ? productDTO.price() : product.price())
                .imageUrl(product.imageUrl())
                .active(productDTO.active())
                .createdAt(product.createdAt())
                .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
                .build();

        productRepository.save(updatedProduct);

        if (productDTO.quantity() != null) {
            stockRepository.findByProductId(productDTO.id()).ifPresent(stock -> {
                stock.setQuantity(productDTO.quantity());
                stock.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
                stockRepository.save(stock);
            });
        }

        log.info("Product with id '" + productDTO.id() + "' has been updated");
        return toDTO(updatedProduct);
    }

    @Transactional
    public ProductDTO deleteById(UUID id) {
        var product = getProductDTOById(id);
        stockRepository.findByProductId(id).ifPresent(stockRepository::delete);
        productRepository.deleteById(id);
        log.info("Product with id '" + id + "' has been deleted");
        return product;
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
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
