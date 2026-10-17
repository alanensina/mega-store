package com.alanensina.order.services;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.enums.ReservationStatus;
import com.alanensina.order.exceptions.IdempotencyRequestInProgressException;
import com.alanensina.order.models.CartIdempotencyRecord;
import com.alanensina.order.repositories.CartIdempotencyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartIdempotencyServiceTest {

    private final CartIdempotencyRepository repository = mock(CartIdempotencyRepository.class);
    private final CartIdempotencyService service = new CartIdempotencyService(repository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executeOrReplayReturnsExistingCartWhenCompleted() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("alice", "pwd", "ROLE_USER"));
        UUID cartId = UUID.randomUUID();
        var existing = CartIdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .requestKey("k1")
                .username("alice")
                .cartId(cartId)
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findByRequestKeyAndUsername("k1", "alice")).thenReturn(Optional.of(existing));

        Function<UUID, CartDTO> replay = id -> cartDTO(id);
        Supplier<CartDTO> createAction = () -> {
            throw new AssertionError("Should not create");
        };

        CartDTO result = service.executeOrReplay("k1", createAction, replay);

        assertEquals(cartId, result.id());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void executeOrReplayThrowsWhenExistingRequestIsInProgress() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("alice", "pwd", "ROLE_USER"));
        var existing = CartIdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .requestKey("k1")
                .username("alice")
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findByRequestKeyAndUsername("k1", "alice")).thenReturn(Optional.of(existing));

        assertThrows(IdempotencyRequestInProgressException.class, () ->
                service.executeOrReplay("k1", () -> cartDTO(UUID.randomUUID()), this::cartDTO));
    }

    @Test
    void executeOrReplayCreatesAndStoresCartIdForNewKey() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("alice", "pwd", "ROLE_USER"));
        UUID createdId = UUID.randomUUID();
        var persisted = CartIdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .requestKey("k1")
                .username("alice")
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByRequestKeyAndUsername("k1", "alice")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(CartIdempotencyRecord.class))).thenReturn(persisted);
        when(repository.save(any(CartIdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));

        CartDTO created = cartDTO(createdId);
        CartDTO result = service.executeOrReplay("k1", () -> created, this::cartDTO);

        assertSame(created, result);
        verify(repository).save(any(CartIdempotencyRecord.class));
    }

    @Test
    void executeOrReplayDeletesReservationWhenCreateFails() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("alice", "pwd", "ROLE_USER"));
        var persisted = CartIdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .requestKey("k1")
                .username("alice")
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findByRequestKeyAndUsername("k1", "alice")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(CartIdempotencyRecord.class))).thenReturn(persisted);

        assertThrows(IllegalStateException.class, () ->
                service.executeOrReplay("k1", () -> {
                    throw new IllegalStateException("failed");
                }, this::cartDTO));

        verify(repository).deleteById(persisted.getId());
    }

    private CartDTO cartDTO(UUID id) {
        return new CartDTO(id, 1L, UUID.randomUUID(), BigDecimal.TEN, ReservationStatus.AWAITING_PAYMENT,
                LocalDateTime.now(), LocalDateTime.now(), List.of());
    }
}
