package com.alanensina.order.models;

import com.alanensina.order.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Entity
@Table(name = "CART")
@AllArgsConstructor
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    private long version;

    // Assigned by the database sequence (atomic under concurrency, never reused)
    @Generated(event = EventType.INSERT)
    @Column(name = "order_number", insertable = false, updatable = false)
    private Long orderNumber;

    private UUID customerId;
    private BigDecimal totalPrice;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items;

}
