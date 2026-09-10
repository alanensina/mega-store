package com.alanensina.inventory.services;

import com.alanensina.inventory.dtos.CreateReservationDTO;
import com.alanensina.inventory.dtos.ReservationDTO;
import com.alanensina.inventory.dtos.ReservationItemDTO;
import com.alanensina.inventory.enums.ReservationStatus;
import com.alanensina.inventory.enums.StockOperation;
import com.alanensina.inventory.exceptions.ProductNotAvailableForReservationException;
import com.alanensina.inventory.exceptions.ReservationAlreadyCancelledException;
import com.alanensina.inventory.exceptions.ReservationNotFoundException;
import com.alanensina.inventory.models.Product;
import com.alanensina.inventory.models.Reservation;
import com.alanensina.inventory.models.ReservationItem;
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
    private final ProductService productService;
    private final StockService stockService;

    @Transactional
    public ReservationDTO create(@Valid CreateReservationDTO reservationDTO) {

        var items = new ArrayList<ReservationItem>();
        var itemsDTO = new ArrayList<ReservationItemDTO>();
        var now = LocalDateTime.now(ZoneId.systemDefault());

        var reservation = Reservation.builder()
                .cartId(reservationDTO.cartId())
                .items(items)
                .reservedAt(now)
                .updatedAt(now)
                .status(ReservationStatus.AWAITING_PAYMENT)
                .build();

        for(ReservationItemDTO itemDTO : reservationDTO.items()) {

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
    public ReservationDTO cancel(UUID id) {
        var reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if(ReservationStatus.CANCELLED.equals(reservation.getStatus())) {
            throw new ReservationAlreadyCancelledException(id);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        reservationRepository.save(reservation);

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

    private ReservationDTO toDTO(Reservation reservation, ArrayList<ReservationItemDTO> itemsDTO) {
        return new ReservationDTO(
                reservation.getId(),
                reservation.getCartId(),
                itemsDTO,
                reservation.getStatus()
        );
    }

    private void removeItemFromStock(ReservationItemDTO itemDTO) {
        stockService.updateStockQuantityByOperation(itemDTO.productId(), itemDTO.quantity(), StockOperation.REMOVE);
    }

    private boolean isProductInStock(ReservationItemDTO itemDTO) {
        return stockService.isProductInStock(itemDTO.productId(), itemDTO.quantity());
    }
}
