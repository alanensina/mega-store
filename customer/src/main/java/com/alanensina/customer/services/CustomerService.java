package com.alanensina.customer.services;

import com.alanensina.customer.dtos.CreateCustomerDTO;
import com.alanensina.customer.dtos.CustomerDTO;
import com.alanensina.customer.dtos.UpdateCustomerDTO;
import com.alanensina.customer.exceptions.CustomerNotFoundException;
import com.alanensina.customer.exceptions.CustomerUsernameOrEmailAlreadyExistsException;
import com.alanensina.customer.models.Customer;
import com.alanensina.customer.repositories.CustomerRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;


@Service
@AllArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncryptionService passwordEncryptionService;

    public CustomerDTO create(@Valid CreateCustomerDTO customerDTO) {

        validateUsernameAndEmail(customerDTO.username(), customerDTO.email());

        var now = LocalDateTime.now(ZoneId.systemDefault());

        var encryptedPassword = passwordEncryptionService.encryptPassword(customerDTO.password());

        var customer = Customer.builder()
                .name(customerDTO.name())
                .email(customerDTO.email())
                .username(customerDTO.username())
                .password(encryptedPassword)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var savedCustomer = customerRepository.save(customer);

        log.info("Customer created with ID: {}", savedCustomer.getId());

        return toDTO(savedCustomer);
    }

    public List<CustomerDTO> list() {
        return customerRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public CustomerDTO getCustomerDTOById(UUID id) {
        var customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return toDTO(customer);
    }

    public Customer getCustomerByUsername(String username) {
        return customerRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new CustomerNotFoundException(username));
    }

    public Customer getCustomerByUsernameToAuthentication(String username) {
        var customer = customerRepository.findByUsernameIgnoreCase(username);
        return customer.orElse(null);
    }

    public CustomerDTO update(@Valid UpdateCustomerDTO customerDTO) {
        var oldCustomer = customerRepository.findById(customerDTO.id())
                .orElseThrow(() -> new CustomerNotFoundException(customerDTO.id()));

        if (customerRepository.existsByUsernameOrEmailForAnotherCustomer(
                customerDTO.username(), customerDTO.email(), customerDTO.id())) {
            throw new CustomerUsernameOrEmailAlreadyExistsException(
                    customerDTO.username(), customerDTO.email());
        }

        var now = LocalDateTime.now(ZoneId.systemDefault());

        oldCustomer.setName(customerDTO.name());
        oldCustomer.setEmail(customerDTO.email());
        oldCustomer.setUsername(customerDTO.username());
        if (customerDTO.password() != null) {
            oldCustomer.setPassword(passwordEncryptionService.encryptPassword(customerDTO.password()));
        }
        oldCustomer.setUpdatedAt(now);

        customerRepository.save(oldCustomer);

        log.info("Customer updated with ID: {}", oldCustomer.getId());

        return toDTO(oldCustomer);
    }

    public CustomerDTO toDTO(Customer savedCustomer) {
        return new CustomerDTO(
                savedCustomer.getId(),
                savedCustomer.getName(),
                savedCustomer.getEmail(),
                savedCustomer.getUsername(),
                savedCustomer.getCreatedAt(),
                savedCustomer.getUpdatedAt(),
                savedCustomer.isAdmin()
        );
    }

    private void validateUsernameAndEmail(String username, String email) {
        if (customerRepository.existsByUsernameIgnoreCaseOrEmailIgnoreCase(username, email)) {
            throw new CustomerUsernameOrEmailAlreadyExistsException(username, email);
        }
    }


    public void delete(UUID id) {
        customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customerRepository.deleteById(id);
        log.info("Customer deleted with ID: {}", id);
    }
}
