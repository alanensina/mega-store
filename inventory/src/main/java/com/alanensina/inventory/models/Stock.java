package com.alanensina.inventory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Entity
@Table(name = "STOCK")
@AllArgsConstructor
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "product_id")
    private Product product;
    private int quantity;
    @Version
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
