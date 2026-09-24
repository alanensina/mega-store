package com.alanensina.customer.services;

import com.alanensina.customer.dtos.CreateCustomerDTO;
import com.alanensina.customer.dtos.CustomerDTO;
import com.alanensina.customer.dtos.UpdateCustomerDTO;
import com.alanensina.customer.exceptions.CustomerNotFoundException;
import com.alanensina.customer.exceptions.CustomerUsernameOrEmailAlreadyExistsException;
import com.alanensina.customer.models.Customer;
import com.alanensina.customer.repositories.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncryptionService passwordEncryptionService;

    @InjectMocks
    private CustomerService customerService;

    private UUID customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        createdAt = LocalDateTime.of(2024, 1, 2, 10, 30, 0);
        updatedAt = LocalDateTime.of(2024, 1, 3, 11, 45, 0);
    }

    @Test
    void createShouldCreateCustomerAndReturnDto() {
        CreateCustomerDTO dto = new CreateCustomerDTO("Alice", "alice@email.com", "alice", "Password123");
        when(customerRepository.existsByUsernameIgnoreCaseOrEmailIgnoreCase("alice", "alice@email.com")).thenReturn(false);
        when(passwordEncryptionService.encryptPassword("Password123")).thenReturn("encrypted-password");

        Customer savedCustomer = Customer.builder()
                .id(customerId)
                .name("Alice")
                .email("alice@email.com")
                .username("alice")
                .password("encrypted-password")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerDTO result = customerService.create(dto);

        assertNotNull(result);
        assertEquals(customerId, result.id());
        assertEquals("Alice", result.name());
        assertEquals("alice@email.com", result.email());
        assertEquals("alice", result.username());
        assertEquals(createdAt, result.createdAt());
        assertEquals(updatedAt, result.updatedAt());

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals("Alice", capturedCustomer.getName());
        assertEquals("alice@email.com", capturedCustomer.getEmail());
        assertEquals("alice", capturedCustomer.getUsername());
        assertEquals("encrypted-password", capturedCustomer.getPassword());
        assertNotNull(capturedCustomer.getCreatedAt());
        assertNotNull(capturedCustomer.getUpdatedAt());

        verify(passwordEncryptionService).encryptPassword("Password123");
    }

    @Test
    void createShouldThrowWhenUsernameOrEmailAlreadyExists() {
        CreateCustomerDTO dto = new CreateCustomerDTO("Alice", "alice@email.com", "alice", "Password123");
        when(customerRepository.existsByUsernameIgnoreCaseOrEmailIgnoreCase("alice", "alice@email.com")).thenReturn(true);

        CustomerUsernameOrEmailAlreadyExistsException exception = assertThrows(
                CustomerUsernameOrEmailAlreadyExistsException.class,
                () -> customerService.create(dto)
        );

        assertTrue(exception.getMessage().contains("alice"));
        assertTrue(exception.getMessage().contains("alice@email.com"));
        verify(passwordEncryptionService, never()).encryptPassword(any());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void listShouldReturnMappedCustomers() {
        Customer customer1 = buildCustomer("Alice", "alice@email.com", "alice");
        Customer customer2 = buildCustomer("Bob", "bob@email.com", "bob");
        when(customerRepository.findAll()).thenReturn(List.of(customer1, customer2));

        List<CustomerDTO> result = customerService.list();

        assertEquals(2, result.size());
        assertEquals(customer1.getId(), result.get(0).id());
        assertEquals("Alice", result.get(0).name());
        assertEquals(customer2.getId(), result.get(1).id());
        assertEquals("Bob", result.get(1).name());
    }

    @Test
    void getCustomerDTOByIdShouldReturnCustomerDto() {
        Customer customer = buildCustomer("Alice", "alice@email.com", "alice");
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        CustomerDTO result = customerService.getCustomerDTOById(customerId);

        assertEquals(customerId, result.id());
        assertEquals("Alice", result.name());
        assertEquals("alice@email.com", result.email());
        assertEquals("alice", result.username());
        assertEquals(customer.getCreatedAt(), result.createdAt());
        assertEquals(customer.getUpdatedAt(), result.updatedAt());
    }

    @Test
    void getCustomerDTOByIdShouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class,
                () -> customerService.getCustomerDTOById(customerId)
        );

        assertTrue(exception.getMessage().contains(customerId.toString()));
    }

    @Test
    void getCustomerByUsernameShouldReturnCustomer() {
        Customer customer = buildCustomer("Alice", "alice@email.com", "alice");
        when(customerRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerByUsername("alice");

        assertEquals(customerId, result.getId());
        assertEquals("alice", result.getUsername());
        assertEquals("alice@email.com", result.getEmail());
    }

    @Test
    void getCustomerByUsernameShouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class,
                () -> customerService.getCustomerByUsername("missing")
        );

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    void updateShouldUpdateCustomerAndReturnDto() {
        Customer existingCustomer = buildCustomer("Alice", "alice@email.com", "alice");
        UpdateCustomerDTO dto = new UpdateCustomerDTO(customerId, "Alice Updated", "alice.updated@email.com", "aliceupdated", "NewPassword123");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByUsernameOrEmailForAnotherCustomer("aliceupdated", "alice.updated@email.com", customerId)).thenReturn(false);
        when(passwordEncryptionService.encryptPassword("NewPassword123")).thenReturn("new-encrypted-password");
        when(customerRepository.save(existingCustomer)).thenReturn(existingCustomer);

        CustomerDTO result = customerService.update(dto);

        assertEquals(customerId, result.id());
        assertEquals("Alice Updated", result.name());
        assertEquals("alice.updated@email.com", result.email());
        assertEquals("aliceupdated", result.username());
        assertEquals(existingCustomer.getCreatedAt(), result.createdAt());
        assertNotNull(result.updatedAt());

        assertEquals("Alice Updated", existingCustomer.getName());
        assertEquals("alice.updated@email.com", existingCustomer.getEmail());
        assertEquals("aliceupdated", existingCustomer.getUsername());
        assertEquals("new-encrypted-password", existingCustomer.getPassword());
        assertNotNull(existingCustomer.getUpdatedAt());

        verify(passwordEncryptionService).encryptPassword("NewPassword123");
    }

    @Test
    void updateShouldThrowWhenAnotherCustomerHasSameUsernameOrEmail() {
        Customer existingCustomer = buildCustomer("Alice", "alice@email.com", "alice");
        UpdateCustomerDTO dto = new UpdateCustomerDTO(customerId, "Alice Updated", "alice.updated@email.com", "aliceupdated", "NewPassword123");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByUsernameOrEmailForAnotherCustomer("aliceupdated", "alice.updated@email.com", customerId)).thenReturn(true);

        CustomerUsernameOrEmailAlreadyExistsException exception = assertThrows(
                CustomerUsernameOrEmailAlreadyExistsException.class,
                () -> customerService.update(dto)
        );

        assertTrue(exception.getMessage().contains("aliceupdated"));
        assertTrue(exception.getMessage().contains("alice.updated@email.com"));
        verify(passwordEncryptionService, never()).encryptPassword(any());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void updateShouldThrowWhenCustomerDoesNotExist() {
        UpdateCustomerDTO dto = new UpdateCustomerDTO(customerId, "Alice", "alice@email.com", "alice", "Password123");
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class,
                () -> customerService.update(dto)
        );

        assertTrue(exception.getMessage().contains(customerId.toString()));
        verify(passwordEncryptionService, never()).encryptPassword(any());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void toDTOShouldMapCustomerFields() {
        Customer customer = buildCustomer("Alice", "alice@email.com", "alice");

        CustomerDTO result = customerService.toDTO(customer);

        assertEquals(customer.getId(), result.id());
        assertEquals(customer.getName(), result.name());
        assertEquals(customer.getEmail(), result.email());
        assertEquals(customer.getUsername(), result.username());
        assertEquals(customer.getCreatedAt(), result.createdAt());
        assertEquals(customer.getUpdatedAt(), result.updatedAt());
    }

    @Test
    void deleteShouldDeleteCustomer() {
        Customer customer = buildCustomer("Alice", "alice@email.com", "alice");
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        customerService.delete(customerId);

        verify(customerRepository).deleteById(customerId);
    }

    @Test
    void deleteShouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class,
                () -> customerService.delete(customerId)
        );

        assertTrue(exception.getMessage().contains(customerId.toString()));
        verify(customerRepository, never()).deleteById(any(UUID.class));
    }

    private Customer buildCustomer(String name, String email, String username) {
        return Customer.builder()
                .id(customerId)
                .name(name)
                .email(email)
                .username(username)
                .password("hashed-password")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
