package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.CreateStockDTO;
import com.alanensina.inventory.dtos.StockDTO;
import com.alanensina.inventory.dtos.UpdateStockDTO;
import com.alanensina.inventory.enums.StockOperation;
import com.alanensina.inventory.exceptions.*;
import com.alanensina.inventory.models.Product;
import com.alanensina.inventory.models.Stock;
import com.alanensina.inventory.repositories.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private StockService stockService;

    private UUID productId;
    private UUID stockId;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        stockId = UUID.randomUUID();
        product = Product.builder().id(productId).name("Keyboard").active(true).build();
    }

    @Test
    void createShouldSaveAndReturnDto() {
        when(productService.getProductById(productId)).thenReturn(product);
        Stock saved = stock(10);
        when(stockRepository.save(any(Stock.class))).thenReturn(saved);

        StockDTO result = stockService.create(new CreateStockDTO(productId, 10));

        assertEquals(stockId, result.id());
        assertSame(product, result.product());
        assertEquals(10, result.quantity());
        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertSame(product, captor.getValue().getProduct());
        assertEquals(10, captor.getValue().getQuantity());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void listShouldMapStocks() {
        when(stockRepository.findAll()).thenReturn(List.of(stock(4), stock(8)));

        List<StockDTO> result = stockService.list();

        assertEquals(List.of(4, 8), result.stream().map(StockDTO::quantity).toList());
    }

    @Test
    void getShouldReturnStockDto() {
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock(7)));

        StockDTO result = stockService.getStockById(stockId);

        assertEquals(stockId, result.id());
        assertEquals(7, result.quantity());
    }

    @Test
    void getShouldThrowWhenMissing() {
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());

        assertThrows(StockNotFoundException.class, () -> stockService.getStockById(stockId));
    }

    @Test
    void updateShouldChangeProductQuantityAndReturnDto() {
        Stock existing = stock(3);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(existing));
        when(productService.getProductById(productId)).thenReturn(product);
        when(stockRepository.save(existing)).thenReturn(existing);

        StockDTO result = stockService.updateStock(new UpdateStockDTO(stockId, productId, 12));

        assertSame(product, existing.getProduct());
        assertEquals(12, existing.getQuantity());
        assertEquals(12, result.quantity());
        assertNotNull(existing.getUpdatedAt());
    }

    @Test
    void updateShouldThrowWhenStockMissing() {
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());

        Executable updateCall = () -> stockService.updateStock(new UpdateStockDTO(stockId, productId, 12));

        assertThrows(StockNotFoundException.class, updateCall);
        verifyNoInteractions(productService);
    }

    @Test
    void deleteShouldDeleteAndReturnStock() {
        Stock stock = stock(5);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        StockDTO result = stockService.deleteById(stockId);

        assertEquals(5, result.quantity());
        verify(stockRepository).delete(stock);
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());

        assertThrows(StockNotFoundException.class, () -> stockService.deleteById(stockId));
        verify(stockRepository, never()).delete(any(Stock.class));
    }

    @Test
    void updateQuantityShouldAdd() {
        when(stockRepository.updateQuantityIfAvailable(productId, 4)).thenReturn(1);

        stockService.updateStockQuantityByOperation(productId, 4, StockOperation.ADD);

        verify(stockRepository).updateQuantityIfAvailable(productId, 4);
        verify(stockRepository, never()).findByProductId(any());
    }

    @Test
    void updateQuantityShouldRemove() {
        when(stockRepository.updateQuantityIfAvailable(productId, -4)).thenReturn(1);

        stockService.updateStockQuantityByOperation(productId, 4, StockOperation.REMOVE);

        verify(stockRepository).updateQuantityIfAvailable(productId, -4);
    }

    @Test
    void updateQuantityShouldRejectNonPositiveQuantity() {
        assertThrows(InvalidQuantityException.class,
                () -> stockService.updateStockQuantityByOperation(productId, 0, StockOperation.ADD));
        verifyNoInteractions(stockRepository);
    }

    @Test
    void updateQuantityShouldRejectUnknownOperation() {
        assertThrows(InvalidStockOperationException.class,
                () -> stockService.updateStockQuantityByOperation(productId, 2, null));
        verify(stockRepository, never()).updateQuantityIfAvailable(any(), anyInt());
    }

    @Test
    void updateQuantityShouldThrowWhenProductIsNotInStock() {
        when(stockRepository.updateQuantityIfAvailable(productId, -2)).thenReturn(0);
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundInStockException.class,
                () -> stockService.updateStockQuantityByOperation(productId, 2, StockOperation.REMOVE));
    }

    @Test
    void updateQuantityShouldThrowWhenStockIsInsufficient() {
        when(stockRepository.updateQuantityIfAvailable(productId, -2)).thenReturn(0);
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock(1)));

        assertThrows(InsufficientStockForReservationException.class,
                () -> stockService.updateStockQuantityByOperation(productId, 2, StockOperation.REMOVE));
    }

    private Stock stock(int quantity) {
        return Stock.builder()
                .id(stockId)
                .product(product)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
