package com.alanensina.inventory.controllers;

import com.alanensina.inventory.dtos.*;
import com.alanensina.inventory.enums.ReservationStatus;
import com.alanensina.inventory.services.ProductService;
import com.alanensina.inventory.services.ReservationService;
import com.alanensina.inventory.services.StockService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class InventoryControllerTest {

    @Test
    void productControllerDelegatesAllOperations() {
        ProductService service = mock(ProductService.class);
        ProductController controller = new ProductController(service);
        UUID id = UUID.randomUUID();
        ProductDTO product = new ProductDTO(id, "Keyboard", true, LocalDateTime.now(), LocalDateTime.now());
        List<ProductDTO> products = List.of(product);
        CreateProductDTO create = new CreateProductDTO("Keyboard", true);
        UpdateProductDTO update = new UpdateProductDTO(id, "Keyboard", false);
        when(service.create(create)).thenReturn(product);
        when(service.listAllProducts()).thenReturn(products);
        when(service.getProductDTOById(id)).thenReturn(product);
        when(service.updateProduct(update)).thenReturn(product);
        when(service.deleteById(id)).thenReturn(product);

        assertSame(product, controller.create(create).getBody());
        assertSame(products, controller.list().getBody());
        assertSame(product, controller.getProductByID(id).getBody());
        assertSame(product, controller.updateProduct(update).getBody());
        assertSame(product, controller.deleteProduct(id).getBody());

        verify(service).create(create);
        verify(service).listAllProducts();
        verify(service).getProductDTOById(id);
        verify(service).updateProduct(update);
        verify(service).deleteById(id);
    }

    @Test
    void stockControllerDelegatesAllOperations() {
        StockService service = mock(StockService.class);
        StockController controller = new StockController(service);
        UUID id = UUID.randomUUID();
        CreateStockDTO create = new CreateStockDTO(id, 5);
        UpdateStockDTO update = new UpdateStockDTO(id, id, 8);
        StockDTO stock = new StockDTO(id, null, 5);
        List<StockDTO> stocks = List.of(stock);
        when(service.create(create)).thenReturn(stock);
        when(service.list()).thenReturn(stocks);
        when(service.getStockById(id)).thenReturn(stock);
        when(service.updateStock(update)).thenReturn(stock);
        when(service.deleteById(id)).thenReturn(stock);

        assertSame(stock, controller.create(create).getBody());
        assertSame(stocks, controller.list().getBody());
        assertSame(stock, controller.getProductByID(id).getBody());
        assertSame(stock, controller.updateStock(update).getBody());
        assertSame(stock, controller.deleteProduct(id).getBody());

        verify(service).create(create);
        verify(service).list();
        verify(service).getStockById(id);
        verify(service).updateStock(update);
        verify(service).deleteById(id);
    }

    @Test
    void reservationControllerDelegatesReadOperations() {
        ReservationService service = mock(ReservationService.class);
        ReservationController controller = new ReservationController(service);
        UUID id = UUID.randomUUID();
        ReservationDTO reservation = new ReservationDTO(id, UUID.randomUUID(), List.of(),
                ReservationStatus.PAID);
        List<ReservationDTO> reservations = List.of(reservation);
        when(service.list()).thenReturn(reservations);
        when(service.getById(id)).thenReturn(reservation);

        assertSame(reservations, controller.list().getBody());
        assertSame(reservation, controller.getById(id).getBody());

        verify(service).list();
        verify(service).getById(id);
    }
}
