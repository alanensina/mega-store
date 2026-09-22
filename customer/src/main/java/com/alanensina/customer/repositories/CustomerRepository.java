package com.alanensina.customer.repositories;

import com.alanensina.customer.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    @Query("""
            select count(c) > 0 from Customer c
            where (lower(c.username) = lower(:username) or lower(c.email) = lower(:email))
              and c.id <> :id
            """)
    boolean existsByUsernameOrEmailForAnotherCustomer(
            @Param("username") String username,
            @Param("email") String email,
            @Param("id") UUID id);

    Optional<Customer> findByUsernameIgnoreCase(String username);

}
