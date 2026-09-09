package com.alanensina.customer.repositories;

import com.alanensina.customer.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

}
