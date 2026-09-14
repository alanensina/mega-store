package com.alanensina.inventory.controllers;

import com.alanensina.inventory.dtos.ReservationDTO;
import com.alanensina.inventory.services.ReservationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservation")
@AllArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<ReservationDTO>> list() {
        return ResponseEntity.ok(reservationService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }
}
