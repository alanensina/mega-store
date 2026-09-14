package com.alanensina.inventory.repositories;

import com.alanensina.inventory.models.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, UUID> {



}