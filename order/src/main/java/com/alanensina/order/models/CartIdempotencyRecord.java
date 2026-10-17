package com.alanensina.order.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Entity
@Table(
        name = "CART_IDEMPOTENCY",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_idempotency_request_key_username",
                columnNames = {"request_key", "username"})
)
@AllArgsConstructor
@NoArgsConstructor
public class CartIdempotencyRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "request_key", nullable = false, length = 200)
    private String requestKey;

    @Column(name = "username", nullable = false, length = 120)
    private String username;

    @Column(name = "cart_id")
    private UUID cartId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
