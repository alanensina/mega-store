package com.alanensina.inventory.repositories;

import com.alanensina.inventory.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

}