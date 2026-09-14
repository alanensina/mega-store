package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.*;
import com.alanensina.inventory.enums.ReservationStatus;
import com.alanensina.inventory.enums.StockOperation;
import com.alanensina.inventory.exceptions.ProductNotAvailableForReservationException;
import com.alanensina.inventory.exceptions.ReservationAlreadyCancelledException;
import com.alanensina.inventory.exceptions.ReservationNotFoundException;
import com.alanensina.inventory.models.Product;
import com.alanensina.inventory.models.Reservation;
import com.alanensina.inventory.models.ReservationItem;
import com.alanensina.inventory.repositories.ReservationItemRepository;
import com.alanensina.inventory.repositories.ReservationRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ProductService productService;
    private final StockService stockService;

    @Transactional
    public ReservationDTO create(@Valid OrderEventDTO event) {

        var items = new ArrayList<ReservationItem>();
        var itemsDTO = new ArrayList<ReservationItemDTO>();

        var reservation = Reservation.builder()
                .cartId(event.cartId())
                .items(items)
                .reservedAt(event.createdAt())
                .updatedAt(event.updatedAt())
                .status(event.status())
                .build();

        for(ReservationItemEvent itemDTO : event.items()) {

            if(isProductInStock(itemDTO)) {
                Product product = productService.getProductById(itemDTO.productId());

                var item = ReservationItem.builder()
                        .product(product)
                        .quantity(itemDTO.quantity())
                        .reservation(reservation)
                        .build();

                items.add(item);
                itemsDTO.add(new ReservationItemDTO(itemDTO.productId(), itemDTO.quantity()));

                removeItemFromStock(itemDTO);
            } else {
                throw new ProductNotAvailableForReservationException(itemDTO.productId());
            }
        }

        reservation = reservationRepository.save(reservation);
        log.info("Reservation created: {}", reservation);

        return toDTO(reservation, itemsDTO);
    }

    @Transactional
    public ReservationDTO update(@Valid OrderEventDTO event) {

        var items = new ArrayList<ReservationItem>();
        var itemsDTO = new ArrayList<ReservationItemDTO>();

        var oldReservation = reservationRepository.findByCartId(event.cartId())
                .orElseThrow(() -> new ReservationNotFoundException(event.cartId()));

        // Clear existing reservation items and return them to stock
        clearReservationItemsByReservationId(oldReservation.getId());

        oldReservation.setCartId(event.cartId());
        oldReservation.setUpdatedAt(event.updatedAt());
        oldReservation.setStatus(event.status());

        for(ReservationItemEvent itemDTO : event.items()) {

            if(isProductInStock(itemDTO)) {
                Product product = productService.getProductById(itemDTO.productId());

                var item = ReservationItem.builder()
                        .product(product)
                        .quantity(itemDTO.quantity())
                        .reservation(oldReservation)
                        .build();

                items.add(item);
                itemsDTO.add(new ReservationItemDTO(itemDTO.productId(), itemDTO.quantity()));

                removeItemFromStock(itemDTO);
            } else {
                throw new ProductNotAvailableForReservationException(itemDTO.productId());
            }
        }

        oldReservation.setItems(items);
        var updatedReservation = reservationRepository.save(oldReservation);
        log.info("Reservation updated: {}", updatedReservation);

        return toDTO(updatedReservation, itemsDTO);
    }

    @Transactional
    public ReservationDTO cancel(OrderEventDTO event) {
        var reservation = reservationRepository.findByCartId(event.cartId())
                .orElseThrow(() -> new ReservationNotFoundException(event.cartId()));

        if(ReservationStatus.CANCELLED.equals(reservation.getStatus())) {
            throw new ReservationAlreadyCancelledException(event.cartId());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        reservationRepository.save(reservation);

        // Return reserved items to stock
        for(ReservationItem item : reservation.getItems()) {
            stockService.updateStockQuantityByOperation(item.getProduct().getId(), item.getQuantity(), StockOperation.ADD);
        }

        log.info("Reservation cancelled: {}", reservation);

        var itemsDTO = new ArrayList<ReservationItemDTO>();
        for(ReservationItem item : reservation.getItems()) {
            itemsDTO.add(new ReservationItemDTO(item.getProduct().getId(), item.getQuantity()));
        }

        return toDTO(reservation, itemsDTO);
    }

    public List<ReservationDTO> list() {
        return reservationRepository.findAll().stream()
                .map(reservation -> {
                    var itemsDTO = new ArrayList<ReservationItemDTO>();
                    for(ReservationItem item : reservation.getItems()) {
                        itemsDTO.add(new ReservationItemDTO(item.getProduct().getId(), item.getQuantity()));
                    }
                    return toDTO(reservation, itemsDTO);
                })
                .toList();
    }

    public ReservationDTO getById(UUID id) {
        var reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        var itemsDTO = new ArrayList<ReservationItemDTO>();
        for(ReservationItem item : reservation.getItems()) {
            itemsDTO.add(new ReservationItemDTO(item.getProduct().getId(), item.getQuantity()));
        }

        return toDTO(reservation, itemsDTO);
    }

    @Transactional
    private void clearReservationItemsByReservationId(UUID id) {
        var reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        for(ReservationItem item : reservation.getItems()) {
            stockService.updateStockQuantityByOperation(item.getProduct().getId(), item.getQuantity(), StockOperation.ADD);
            reservationItemRepository.delete(item);
        }

        reservation.getItems().clear();
        reservationRepository.save(reservation);

        log.info("Reservation items cleared for reservation ID: {}", id);
    }

    private ReservationDTO toDTO(Reservation reservation, ArrayList<ReservationItemDTO> itemsDTO) {
        return new ReservationDTO(
                reservation.getId(),
                reservation.getCartId(),
                itemsDTO,
                reservation.getStatus()
        );
    }

    private void removeItemFromStock(ReservationItemEvent itemDTO) {
        stockService.updateStockQuantityByOperation(itemDTO.productId(), itemDTO.quantity(), StockOperation.REMOVE);
    }

    private boolean isProductInStock(ReservationItemEvent itemDTO) {
        return stockService.isProductInStock(itemDTO.productId(), itemDTO.quantity());
    }
}
