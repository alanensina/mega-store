package com.alanensina.inventory.repositories;

import com.alanensina.inventory.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByCartId(UUID cartId);

}