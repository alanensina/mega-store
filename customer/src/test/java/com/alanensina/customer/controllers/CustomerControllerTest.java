package com.alanensina.customer.controllers;

import com.alanensina.customer.dtos.CreateCustomerDTO;
import com.alanensina.customer.dtos.CustomerDTO;
import com.alanensina.customer.dtos.UpdateCustomerDTO;
import com.alanensina.customer.services.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController controller;

    @Test
    void shouldDelegateCustomerOperations() {
        UUID id = UUID.randomUUID();
        CustomerDTO customer = new CustomerDTO(id, "Alice", "alice@example.com", "alice",
                LocalDateTime.MIN, LocalDateTime.MAX, false);
        CreateCustomerDTO create = new CreateCustomerDTO("Alice", "alice@example.com", "alice", "Password123");
        UpdateCustomerDTO update = new UpdateCustomerDTO(id, "Alice", "alice@example.com", "alice", "Password123");

        when(customerService.create(create)).thenReturn(customer);
        when(customerService.list()).thenReturn(List.of(customer));
        when(customerService.getCustomerDTOById(id)).thenReturn(customer);
        when(customerService.update(update)).thenReturn(customer);

        assertSame(customer, controller.create(create).getBody());
        assertEquals(List.of(customer), controller.list().getBody());
        assertSame(customer, controller.getById(id).getBody());
        assertSame(customer, controller.update(update).getBody());
        assertEquals(204, controller.delete(id).getStatusCode().value());

        verify(customerService).delete(id);
    }
}
