package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.*;
import com.alanensina.inventory.enums.OrderEventType;
import com.alanensina.inventory.enums.ReservationStatus;
import com.alanensina.inventory.enums.StockOperation;
import com.alanensina.inventory.exceptions.ReservationNotFoundException;
import com.alanensina.inventory.models.Product;
import com.alanensina.inventory.models.Reservation;
import com.alanensina.inventory.models.ReservationItem;
import com.alanensina.inventory.repositories.ReservationItemRepository;
import com.alanensina.inventory.repositories.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationItemRepository reservationItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private StockService stockService;

    @InjectMocks
    private ReservationService reservationService;

    private UUID cartId;
    private UUID reservationId;
    private UUID firstProductId;
    private UUID secondProductId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        firstProductId = UUID.randomUUID();
        secondProductId = UUID.randomUUID();
        createdAt = LocalDateTime.of(2024, 1, 2, 10, 30);
        updatedAt = LocalDateTime.of(2024, 1, 3, 11, 45);
    }

    @Test
    void createShouldReturnExistingReservationWithoutChangingStock() {
        Reservation existing = reservation(ReservationStatus.PAID);
        existing.setItems(new ArrayList<>(List.of(item(existing, firstProductId, 2))));
        when(reservationRepository.findByCartId(cartId)).thenReturn(Optional.of(existing));

        ReservationDTO result = reservationService.create(event(itemEvent(firstProductId, 2)));

        assertEquals(reservationId, result.id());
        assertEquals(1, result.items().size());
        assertEquals(firstProductId, result.items().get(0).productId());
        verifyNoInteractions(productService, stockService);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createShouldSortItemsReserveStockAndSave() {
        Product first = product(firstProductId);
        Product second = product(secondProductId);
        when(reservationRepository.findByCartId(cartId)).thenReturn(Optional.empty());
        when(productService.getProductById(firstProductId)).thenReturn(first);
        when(productService.getProductById(secondProductId)).thenReturn(second);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(reservationId);
            return saved;
        });

        ReservationDTO result = reservationService.create(event(
                itemEvent(secondProductId, 4), itemEvent(firstProductId, 2)));

        assertEquals(reservationId, result.id());

        // Verifica os productIds sem importar a ordem
        assertEquals(Set.of(firstProductId, secondProductId),
                result.items().stream().map(ReservationItemDTO::productId).collect(toSet()));

        // Verifica as quantidades sem importar a ordem
        assertEquals(Set.of(2, 4),
                result.items().stream().map(ReservationItemDTO::quantity).collect(toSet()));

        verify(stockService).updateStockQuantityByOperation(firstProductId, 2, StockOperation.REMOVE);
        verify(stockService).updateStockQuantityByOperation(secondProductId, 4, StockOperation.REMOVE);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void updateShouldReplaceItemsAndReturnOldItemsToStock() {
        Reservation old = reservation(ReservationStatus.AWAITING_PAYMENT);
        old.setItems(new ArrayList<>(List.of(item(old, firstProductId, 3))));
        when(reservationRepository.findByCartId(cartId)).thenReturn(Optional.of(old));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(old));
        when(productService.getProductById(secondProductId)).thenReturn(product(secondProductId));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationDTO result = reservationService.update(event(itemEvent(secondProductId, 5)));

        assertEquals(List.of(secondProductId), result.items().stream().map(ReservationItemDTO::productId).toList());
        assertEquals(5, result.items().get(0).quantity());
        assertEquals(ReservationStatus.PAID, result.status());
        verify(stockService).updateStockQuantityByOperation(firstProductId, 3, StockOperation.ADD);
        verify(stockService).updateStockQuantityByOperation(secondProductId, 5, StockOperation.REMOVE);
        verify(reservationItemRepository).delete(any(ReservationItem.class));
        verify(reservationRepository, times(2)).save(old);
    }

    @Test
    void updateShouldThrowWhenReservationDoesNotExist() {
        when(reservationRepository.findByCartId(cartId)).thenReturn(Optional.empty());

        Executable updateCall = () -> reservationService.update(event(itemEvent(firstProductId, 1)));

        assertThrows(ReservationNotFoundException.class, updateCall);
        verifyNoInteractions(stockService, productService);
    }

    @Test
    void cancelShouldReturnAlreadyCancelledReservationWithoutSaving() {
        Reservation reservation = reservation(ReservationStatus.CANCELLED);
        reservation.setItems(new ArrayList<>(List.of(item(reservation, firstProductId, 2))));
        when(reservationRepository.findByCartId(cartId)).thenReturn(Optional.of(reservation));

        ReservationDTO result = reservationService.cancel(event(itemEvent(firstProductId, 2)));

        assertEquals(ReservationStatus.CANCELLED, result.status());
        assertEquals(2, result.items().get(0).quantity());
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(stockService);
    }

    @Test
    void cancelShouldMarkReservationAndReturnItemsToStock() {
        Reservation reservation = reservation(ReservationStatus.PAID);
        reservation.setItems(new ArrayList<>(List.of(item(reservation, firstProductId, 2),
                item(reservation, secondProductId, 4))));
        when(reservationRepository.findByCartId(cartId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationDTO result = reservationService.cancel(event(itemEvent(firstProductId, 2)));

        assertEquals(ReservationStatus.CANCELLED, result.status());
        assertEquals(2, result.items().size());
        verify(stockService).updateStockQuantityByOperation(firstProductId, 2, StockOperation.ADD);
        verify(stockService).updateStockQuantityByOperation(secondProductId, 4, StockOperation.ADD);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelShouldThrowWhenReservationDoesNotExist() {
        when(reservationRepository.findByCartId(cartId)).thenReturn(Optional.empty());

        Executable cancelCall = () -> reservationService.cancel(event(itemEvent(firstProductId, 1)));

        assertThrows(ReservationNotFoundException.class, cancelCall);
        verifyNoInteractions(stockService);
    }

    @Test
    void listShouldMapReservationsAndItems() {
        Reservation first = reservation(ReservationStatus.PAID);
        first.setItems(new ArrayList<>(List.of(item(first, firstProductId, 1))));
        Reservation second = reservation(ReservationStatus.DELIVERED);
        second.setItems(new ArrayList<>());
        when(reservationRepository.findAll()).thenReturn(List.of(first, second));

        List<ReservationDTO> result = reservationService.list();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).items().size());
        assertTrue(result.get(1).items().isEmpty());
    }

    @Test
    void getByIdShouldMapReservationAndItems() {
        Reservation reservation = reservation(ReservationStatus.PAID);
        reservation.setItems(new ArrayList<>(List.of(item(reservation, firstProductId, 6))));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        ReservationDTO result = reservationService.getById(reservationId);

        assertEquals(reservationId, result.id());
        assertEquals(cartId, result.cartId());
        assertEquals(firstProductId, result.items().get(0).productId());
        assertEquals(6, result.items().get(0).quantity());
    }

    @Test
    void getByIdShouldThrowWhenMissing() {
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.getById(reservationId));
    }

    private OrderEventDTO event(ReservationItemEvent... items) {
        return new OrderEventDTO(OrderEventType.CREATE, cartId, List.of(items),
                ReservationStatus.PAID, createdAt, updatedAt);
    }

    private ReservationItemEvent itemEvent(UUID productId, int quantity) {
        return new ReservationItemEvent(productId, quantity);
    }

    private Reservation reservation(ReservationStatus status) {
        return Reservation.builder()
                .id(reservationId)
                .cartId(cartId)
                .status(status)
                .reservedAt(createdAt)
                .updatedAt(updatedAt)
                .items(new ArrayList<>())
                .build();
    }

    private ReservationItem item(Reservation reservation, UUID productId, int quantity) {
        return ReservationItem.builder()
                .reservation(reservation)
                .product(product(productId))
                .quantity(quantity)
                .build();
    }

    private Product product(UUID id) {
        return Product.builder().id(id).name("Product " + id).active(true).build();
    }
}
