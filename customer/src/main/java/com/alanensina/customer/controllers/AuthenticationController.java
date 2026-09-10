package com.alanensina.customer.controllers;

import com.alanensina.customer.dtos.CustomerDTO;
import com.alanensina.customer.dtos.LoginDTO;
import com.alanensina.customer.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping()
    public ResponseEntity<CustomerDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(authenticationService.login(loginDTO));
    }
}
