package com.alanensina.inventory.repositories;

import com.alanensina.inventory.models.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductId(UUID productId);

    @Modifying
    @Query("""
            update Stock s
            set s.quantity = s.quantity + :quantity,
                s.version = s.version + 1,
                s.updatedAt = CURRENT_TIMESTAMP
            where s.product.id = :productId
              and s.quantity + :quantity >= 0
            """)
    int updateQuantityIfAvailable(
            @Param("productId") UUID productId,
            @Param("quantity") int quantity);
}