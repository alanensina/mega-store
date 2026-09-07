package com.alanensina.inventory.repositories;

import com.alanensina.inventory.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByNameIgnoreCase(String name);
}