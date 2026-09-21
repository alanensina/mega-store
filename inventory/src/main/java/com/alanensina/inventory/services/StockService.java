package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.CreateStockDTO;
import com.alanensina.inventory.dtos.StockDTO;
import com.alanensina.inventory.dtos.UpdateStockDTO;
import com.alanensina.inventory.enums.StockOperation;
import com.alanensina.inventory.exceptions.*;
import com.alanensina.inventory.models.Stock;
import com.alanensina.inventory.repositories.StockRepository;
import jakarta.transaction.Transactional;
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
public class StockService {

    private final StockRepository stockRepository;
    private final ProductService productService;

    public StockDTO create(@Valid CreateStockDTO stockDTO) {

        var product = productService.getProductById(stockDTO.productId());
        var now = LocalDateTime.now(ZoneId.systemDefault());

        var stock = new Stock();
        stock.setProduct(product);
        stock.setQuantity(stockDTO.quantity());
        stock.setCreatedAt(now);
        stock.setUpdatedAt(now);
        var savedStock = stockRepository.save(stock);
        log.info("Stock created for product {} with quantity {}", product.getName(), stockDTO.quantity());

        return toDTO(savedStock);
    }

    public List<StockDTO> list() {
        return stockRepository
                .findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public StockDTO getStockById(UUID id) {
        var stock = stockRepository.findById(id).orElseThrow(() -> new StockNotFoundException(id));
        return toDTO(stock);
    }

    @Transactional
    public StockDTO updateStock(@Valid UpdateStockDTO stockDTO) {
        var stock = stockRepository.findById(stockDTO.id()).orElseThrow(() -> new StockNotFoundException(stockDTO.id()));
        var product = productService.getProductById(stockDTO.productId());

        stock.setProduct(product);
        stock.setQuantity(stockDTO.quantity());
        stock.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        var updatedStock = stockRepository.save(stock);

        log.info("Stock updated for product {} with quantity {}", product.getName(), stockDTO.quantity());
        return toDTO(updatedStock);
    }

    public StockDTO deleteById(UUID id) {
        var stock = stockRepository.findById(id).orElseThrow(() -> new StockNotFoundException(id));
        stockRepository.delete(stock);
        log.info("Stock deleted for product {}", stock.getProduct().getName());
        return toDTO(stock);
    }

    @Transactional
    public void updateStockQuantityByOperation(UUID productId, int quantity, StockOperation operation) {
        if (quantity <= 0) {
            throw new InvalidQuantityException();
        }

        int quantityDelta;
        if (StockOperation.ADD.equals(operation)) {
            quantityDelta = quantity;
        } else if (StockOperation.REMOVE.equals(operation)) {
            quantityDelta = -quantity;
        } else {
            throw new InvalidStockOperationException(operation);
        }

        if (stockRepository.updateQuantityIfAvailable(productId, quantityDelta) == 1) {
            return;
        }

        if (stockRepository.findByProductId(productId).isEmpty()) {
            throw new ProductNotFoundInStockException(productId);
        }

        throw new InsufficientStockForReservationException(productId);
    }

    private StockDTO toDTO(Stock savedStock) {
        return new StockDTO(
                savedStock.getId(),
                savedStock.getProduct(),
                savedStock.getQuantity()
        );
    }
}
