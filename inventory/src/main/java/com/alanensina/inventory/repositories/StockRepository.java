package com.alanensina.inventory.repositories;

import com.alanensina.inventory.models.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    boolean existsByProduct_IdAndQuantityGreaterThanEqual(UUID productId, int quantity);
    Optional<Stock> findByProduct_Id(UUID productId);
}