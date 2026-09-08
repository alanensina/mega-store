package com.alanensina.inventory.controllers;

import com.alanensina.inventory.dtos.CreateReservationDTO;
import com.alanensina.inventory.dtos.ReservationDTO;
import com.alanensina.inventory.services.ReservationService;
import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<ReservationDTO> create(@Valid @RequestBody CreateReservationDTO reservationDTO) {
        return ResponseEntity.ok(reservationService.create(reservationDTO));
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<ReservationDTO> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @GetMapping
    public ResponseEntity<List<ReservationDTO>> list() {
        return ResponseEntity.ok(reservationService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }


}
