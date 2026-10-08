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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        createdAt = LocalDateTime.of(2024, 1, 2, 10, 30);
        updatedAt = LocalDateTime.of(2024, 1, 3, 11, 45);
    }

    @Test
    void createShouldSaveAndReturnDto() {
        CreateProductDTO input = new CreateProductDTO("Keyboard", BigDecimal.TEN, true, 5);
        Product saved = product("Keyboard", true);
        when(productRepository.existsByNameIgnoreCase("Keyboard")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductDTO result = productService.create(input);

        assertEquals(productId, result.id());
        assertEquals("Keyboard", result.name());
        assertTrue(result.active());
        verify(productRepository).save(any(Product.class));
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("Keyboard", captor.getValue().getName());
        assertNotNull(captor.getValue().getCreatedAt());
        assertNotNull(captor.getValue().getUpdatedAt());
        assertEquals(BigDecimal.TEN, captor.getValue().getPrice());
        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(stockCaptor.capture());
        assertEquals(5, stockCaptor.getValue().getQuantity());
        assertEquals(saved, stockCaptor.getValue().getProduct());
    }

    @Test
    void createShouldRejectDuplicateName() {
        when(productRepository.existsByNameIgnoreCase("Keyboard")).thenReturn(true);

        Executable createCall = () -> productService.create(new CreateProductDTO("Keyboard", BigDecimal.TEN, true, 5));

        assertThrows(ProductNameAlreadyExistsException.class, createCall);
        verify(productRepository, never()).save(any());
        verify(stockRepository, never()).save(any());
    }

    @Test
    void listShouldMapProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product("Keyboard", true), product("Mouse", false)));

        List<ProductDTO> result = productService.listAllProducts();

        assertEquals(List.of("Keyboard", "Mouse"), result.stream().map(ProductDTO::name).toList());
        assertFalse(result.get(1).active());
    }

    @Test
    void getProductDtoByIdShouldReturnMappedProduct() {
        Product product = product("Keyboard", true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductDTO result = productService.getProductDTOById(productId);

        assertEquals(productId, result.id());
        assertEquals(createdAt, result.createdAt());
        assertEquals(updatedAt, result.updatedAt());
    }

    @Test
    void getProductDtoByIdShouldThrowWhenMissing() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductDTOById(productId));
    }

    @Test
    void getProductByIdShouldReturnEntity() {
        Product product = product("Keyboard", true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertSame(product, productService.getProductById(productId));
    }

    @Test
    void getProductByIdShouldThrowWhenMissing() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(productId));
    }

    @Test
    void updateShouldPreserveCreationTimeAndSaveChanges() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product("Old name", true)));
        when(productRepository.existsByNameIgnoreCase("New name")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDTO result = productService.updateProduct(new UpdateProductDTO(productId, "New name", false));

        assertEquals(productId, result.id());
        assertEquals("New name", result.name());
        assertFalse(result.active());
        assertEquals(createdAt, result.createdAt());
        assertNotNull(result.updatedAt());
    }

    @Test
    void updateShouldRejectDuplicateName() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product("Old name", true)));
        when(productRepository.existsByNameIgnoreCase("New name")).thenReturn(true);

        Executable updateCall = () -> productService.updateProduct(new UpdateProductDTO(productId, "New name", false));

        assertThrows(ProductNameAlreadyExistsException.class, updateCall);
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteShouldReturnAndDeleteProduct() {
        Product product = product("Keyboard", true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductDTO result = productService.deleteById(productId);

        assertEquals(productId, result.id());
        verify(productRepository).deleteById(productId);
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteById(productId));
        verify(productRepository, never()).deleteById(any());
    }

    private Product product(String name, boolean active) {
        return Product.builder()
                .id(productId)
                .name(name)
                .price(BigDecimal.TEN)
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
