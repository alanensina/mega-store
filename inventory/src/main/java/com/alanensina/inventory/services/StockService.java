package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.CreateStockDTO;
import com.alanensina.inventory.dtos.StockDTO;
import com.alanensina.inventory.dtos.UpdateStockDTO;
import com.alanensina.inventory.enums.StockOperation;
import com.alanensina.inventory.exceptions.InsufficientStockForReservationException;
import com.alanensina.inventory.exceptions.InvalidStockOperationException;
import com.alanensina.inventory.exceptions.ProductNotFoundInStockException;
import com.alanensina.inventory.exceptions.StockNotFoundException;
import com.alanensina.inventory.models.Stock;
import com.alanensina.inventory.repositories.StockRepository;
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

    public StockDTO updateStock(UUID id, @Valid UpdateStockDTO stockDTO) {
        var stock = stockRepository.findById(id).orElseThrow(() -> new StockNotFoundException(id));
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

    public void updateStockQuantityByOperation(UUID productId, int quantity, StockOperation operation) {
        var stock = stockRepository.findByProduct_Id(productId).orElseThrow(() -> new ProductNotFoundInStockException(productId));

        if(StockOperation.ADD.equals(operation)) {
            stock.setQuantity(stock.getQuantity() + quantity);
        }
        else if(StockOperation.REMOVE.equals(operation)) {
            var newQuantity = stock.getQuantity() - quantity;

            if(newQuantity < 0) {
                throw new InsufficientStockForReservationException(productId);
            }

            stock.setQuantity(newQuantity);

        } else {
            throw new InvalidStockOperationException(operation);
        }

        stock.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        stockRepository.save(stock);
    }

    public boolean isProductInStock(UUID productId, int quantity) {
        return stockRepository.existsByProduct_IdAndQuantityGreaterThanEqual(productId, quantity);
    }

    private StockDTO toDTO(Stock savedStock) {
        return new StockDTO(
                savedStock.getId(),
                savedStock.getProduct(),
                savedStock.getQuantity()
        );
    }
}
