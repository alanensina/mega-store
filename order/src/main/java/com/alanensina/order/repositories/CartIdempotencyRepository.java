package com.alanensina.order.repositories;

import com.alanensina.order.models.CartIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartIdempotencyRepository extends JpaRepository<CartIdempotencyRecord, UUID> {

    Optional<CartIdempotencyRecord> findByRequestKeyAndUsername(String requestKey, String username);
}
