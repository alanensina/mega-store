package com.alanensina.customer.controllers;

import com.alanensina.customer.dtos.CreateCustomerDTO;
import com.alanensina.customer.dtos.CustomerDTO;
import com.alanensina.customer.dtos.UpdateCustomerDTO;
import com.alanensina.customer.services.CustomerService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
@AllArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerDTO> create(@Valid @RequestBody CreateCustomerDTO customerDTO) {
        return ResponseEntity.ok(customerService.create(customerDTO));
    }

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> list() {
        return ResponseEntity.ok(customerService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomerDTOById(id));
    }

    @PutMapping
    public ResponseEntity<CustomerDTO> update(@Valid @RequestBody UpdateCustomerDTO customerDTO) {
        return ResponseEntity.ok(customerService.update(customerDTO));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
