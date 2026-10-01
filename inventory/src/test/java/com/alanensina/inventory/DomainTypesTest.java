package com.alanensina.inventory;

import com.alanensina.inventory.dtos.*;
import com.alanensina.inventory.enums.OrderEventType;
import com.alanensina.inventory.enums.ReservationStatus;
import com.alanensina.inventory.enums.StockOperation;
import com.alanensina.inventory.exceptions.*;
import com.alanensina.inventory.models.Product;
import com.alanensina.inventory.models.Reservation;
import com.alanensina.inventory.models.ReservationItem;
import com.alanensina.inventory.models.Stock;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainTypesTest {

    @Test
    void dtoRecordsExposeValuesAndValueSemantics() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 2, 10, 30);
        var createProduct = new CreateProductDTO("Keyboard", true);
        var createStock = new CreateStockDTO(id, 3);
        var createReservation = new CreateReservationDTO(id, List.of(new ReservationItemDTO(id, 3)),
                ReservationStatus.PAID, now, now);
        var orderEvent = new OrderEventDTO(OrderEventType.CREATE, id,
                List.of(new ReservationItemEvent(id, 3)), ReservationStatus.PAID, now, now);
        var product = new ProductDTO(id, "Keyboard", true, now, now);
        var reservation = new ReservationDTO(id, id, List.of(new ReservationItemDTO(id, 3)),
                ReservationStatus.PAID);
        var reservationItem = new ReservationItemDTO(id, 3);
        var reservationItemEvent = new ReservationItemEvent(id, 3);
        var stock = new StockDTO(id, null, 3);
        var updateProduct = new UpdateProductDTO(id, "Keyboard", true);
        var updateStock = new UpdateStockDTO(id, id, 3);

        assertAll(
                () -> assertEquals("Keyboard", createProduct.name()),
                () -> assertTrue(createProduct.active()),
                () -> assertEquals(id, createStock.productId()),
                () -> assertEquals(3, createStock.quantity()),
                () -> assertEquals(id, createReservation.cartId()),
                () -> assertEquals(ReservationStatus.PAID, createReservation.status()),
                () -> assertEquals(now, createReservation.createdAt()),
                () -> assertEquals(now, createReservation.updatedAt()),
                () -> assertEquals(OrderEventType.CREATE, orderEvent.eventType()),
                () -> assertEquals(id, orderEvent.cartId()),
                () -> assertEquals(3, orderEvent.items().getFirst().quantity()),
                () -> assertEquals(ReservationStatus.PAID, orderEvent.status()),
                () -> assertEquals(now, orderEvent.createdAt()),
                () -> assertEquals(now, orderEvent.updatedAt()),
                () -> assertEquals(id, product.id()),
                () -> assertEquals("Keyboard", product.name()),
                () -> assertTrue(product.active()),
                () -> assertEquals(now, product.createdAt()),
                () -> assertEquals(now, product.updatedAt()),
                () -> assertEquals(id, reservation.id()),
                () -> assertEquals(id, reservation.cartId()),
                () -> assertEquals(ReservationStatus.PAID, reservation.status()),
                () -> assertEquals(id, reservationItem.productId()),
                () -> assertEquals(3, reservationItem.quantity()),
                () -> assertEquals(id, reservationItemEvent.productId()),
                () -> assertEquals(3, reservationItemEvent.quantity()),
                () -> assertEquals(id, stock.id()),
                () -> assertNull(stock.product()),
                () -> assertEquals(3, stock.quantity()),
                () -> assertEquals(id, updateProduct.id()),
                () -> assertEquals("Keyboard", updateProduct.name()),
                () -> assertTrue(updateProduct.active()),
                () -> assertEquals(id, updateStock.id()),
                () -> assertEquals(id, updateStock.productId()),
                () -> assertEquals(3, updateStock.quantity())
        );

        assertEquals(new CreateProductDTO("Keyboard", true), createProduct);
        assertEquals(createProduct.hashCode(), new CreateProductDTO("Keyboard", true).hashCode());
        assertNotNull(createProduct.toString());
        assertEquals(new CreateStockDTO(id, 3), createStock);
        assertNotNull(createStock.toString());
        assertEquals(new CreateReservationDTO(id, createReservation.items(), ReservationStatus.PAID, now, now),
                createReservation);
        assertNotNull(createReservation.toString());
        assertEquals(new OrderEventDTO(OrderEventType.CREATE, id, orderEvent.items(),
                ReservationStatus.PAID, now, now), orderEvent);
        assertNotNull(orderEvent.toString());
        assertEquals(new ProductDTO(id, "Keyboard", true, now, now), product);
        assertNotNull(product.toString());
        assertEquals(new ReservationDTO(id, id, reservation.items(), ReservationStatus.PAID), reservation);
        assertNotNull(reservation.toString());
        assertEquals(new ReservationItemDTO(id, 3), reservationItem);
        assertNotNull(reservationItem.toString());
        assertEquals(new ReservationItemEvent(id, 3), reservationItemEvent);
        assertNotNull(reservationItemEvent.toString());
        assertEquals(new StockDTO(id, null, 3), stock);
        assertNotNull(stock.toString());
        assertEquals(new UpdateProductDTO(id, "Keyboard", true), updateProduct);
        assertNotNull(updateProduct.toString());
        assertEquals(new UpdateStockDTO(id, id, 3), updateStock);
        assertNotNull(updateStock.toString());
    }

    @Test
    void enumsExposeAllValuesAndLookup() {
        assertArrayEquals(new OrderEventType[]{OrderEventType.CREATE, OrderEventType.UPDATE, OrderEventType.CANCEL},
                OrderEventType.values());
        assertEquals(OrderEventType.CREATE, OrderEventType.valueOf("CREATE"));
        assertArrayEquals(new ReservationStatus[]{ReservationStatus.AWAITING_PAYMENT, ReservationStatus.PAID,
                        ReservationStatus.DELIVERING, ReservationStatus.DELIVERED, ReservationStatus.CANCELLED},
                ReservationStatus.values());
        assertEquals(ReservationStatus.CANCELLED, ReservationStatus.valueOf("CANCELLED"));
        assertArrayEquals(new StockOperation[]{StockOperation.ADD, StockOperation.REMOVE}, StockOperation.values());
        assertEquals(StockOperation.REMOVE, StockOperation.valueOf("REMOVE"));
    }

    @Test
    void lombokModelsSupportConstructionAccessorsAndValueMethods() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 2, 10, 30);

        Product product = Product.builder().id(id).name("Keyboard").active(true)
                .createdAt(now).updatedAt(now).build();
        Product sameProduct = new Product(id, "Keyboard", true, now, now);
        assertEquals(product, sameProduct);
        assertEquals(product.hashCode(), sameProduct.hashCode());
        assertTrue(product.toString().contains("Keyboard"));
        product.setName("Mouse");
        assertEquals("Mouse", product.getName());
        assertTrue(product.isActive());

        Reservation reservation = Reservation.builder().id(id).cartId(id).items(List.of())
                .status(ReservationStatus.PAID).reservedAt(now).updatedAt(now).build();
        Reservation sameReservation = new Reservation(id, id, List.of(), ReservationStatus.PAID, now, now);
        assertEquals(reservation, sameReservation);
        assertEquals(reservation.hashCode(), sameReservation.hashCode());
        assertNotNull(reservation.toString());
        reservation.setStatus(ReservationStatus.CANCELLED);
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(id, reservation.getCartId());

        ReservationItem item = ReservationItem.builder().id(id).reservation(reservation).product(product)
                .quantity(2).build();
        ReservationItem sameItem = new ReservationItem(id, reservation, product, 2);
        assertEquals(item, sameItem);
        assertEquals(item.hashCode(), sameItem.hashCode());
        assertTrue(item.toString().contains("quantity=2"));
        item.setQuantity(4);
        assertEquals(4, item.getQuantity());
        assertSame(reservation, item.getReservation());
        assertSame(product, item.getProduct());

        Stock stock = Stock.builder().id(id).product(product).quantity(5).version(2L)
                .createdAt(now).updatedAt(now).build();
        Stock sameStock = new Stock(id, product, 5, 2L, now, now);
        assertEquals(stock, sameStock);
        assertEquals(stock.hashCode(), sameStock.hashCode());
        assertTrue(stock.toString().contains("quantity=5"));
        stock.setQuantity(7);
        assertEquals(7, stock.getQuantity());
        assertEquals(2L, stock.getVersion());
        assertSame(product, stock.getProduct());
        assertEquals(now, stock.getCreatedAt());
        assertEquals(now, stock.getUpdatedAt());
    }

    @Test
    void exceptionTypesCreateTheirExpectedMessages() {
        UUID id = UUID.randomUUID();

        assertEquals("Product with ID " + id + " not found.", new ProductNotFoundException(id).getMessage());
        assertEquals("Product with name 'Keyboard' already exists",
                new ProductNameAlreadyExistsException("Keyboard").getMessage());
        assertEquals("Stock with ID " + id + " not found.", new StockNotFoundException(id).getMessage());
        assertEquals("Product with ID " + id + " not found in stock.",
                new ProductNotFoundInStockException(id).getMessage());
        assertEquals("Insufficient stock for product with ID " + id + " to fulfill the reservation.",
                new InsufficientStockForReservationException(id).getMessage());
        assertEquals("Reservation with ID " + id + " not found.", new ReservationNotFoundException(id).getMessage());
        assertEquals("Reservation already cancelled with id: " + id,
                new ReservationAlreadyCancelledException(id).getMessage());
        assertEquals("Product with ID " + id + " not available in stock.",
                new ProductNotAvailableForReservationException(id).getMessage());
        assertEquals("Invalid stock operation: null", new InvalidStockOperationException(null).getMessage());
        assertEquals("Stock quantity must be greater than zero", new InvalidQuantityException().getMessage());
        assertEquals("Invalid order event type: UNKNOWN",
                new InvalidKafkaOrderEventTypeException("UNKNOWN").getMessage());
        assertEquals("Error processing Kafka message: failed", new KafkaErrorException("failed").getMessage());
        assertEquals("Failed to configure Spring Security Chain: failed",
                new SecurityFilterChainFailedException("failed").getMessage());
    }
}
