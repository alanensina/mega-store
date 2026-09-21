package com.alanensina.inventory.controllers;

import com.alanensina.inventory.dtos.CreateStockDTO;
import com.alanensina.inventory.dtos.StockDTO;
import com.alanensina.inventory.dtos.UpdateStockDTO;
import com.alanensina.inventory.services.StockService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
@AllArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping
    public ResponseEntity<StockDTO> create(@Valid @RequestBody CreateStockDTO stockDTO) {
        return ResponseEntity.ok(stockService.create(stockDTO));
    }

    @GetMapping
    public ResponseEntity<List<StockDTO>> list() {
        return ResponseEntity.ok(stockService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockDTO> getProductByID(@PathVariable UUID id) {
        return ResponseEntity.ok(stockService.getStockById(id));
    }

    @PutMapping()
    public ResponseEntity<StockDTO> updateStock(@Valid @RequestBody UpdateStockDTO stockDTO) {
        return ResponseEntity.ok(stockService.updateStock(stockDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StockDTO> deleteProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(stockService.deleteById(id));
    }

}
