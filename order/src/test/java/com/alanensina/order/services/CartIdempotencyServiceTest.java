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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataIntegrityViolationException;

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

        Function<UUID, CartDTO> replay = this::cartDTO;
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

    @Test
    void executeOrReplayRejectsBlankKeys() {
        assertThrows(IllegalArgumentException.class, () ->
                service.executeOrReplay("  ", () -> cartDTO(UUID.randomUUID()), this::cartDTO));
        verifyNoInteractions(repository);
    }

    @Test
    void executeOrReplayUsesAnonymousForUnauthenticatedRequests() {
        UUID createdId = UUID.randomUUID();
        var persisted = CartIdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .requestKey("k1")
                .username("anonymous")
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findByRequestKeyAndUsername("k1", "anonymous")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(CartIdempotencyRecord.class))).thenReturn(persisted);

        assertEquals(createdId, service.executeOrReplay(
                " k1 ",
                () -> cartDTO(createdId),
                this::cartDTO).id());
    }

    @Test
    void executeOrReplayUsesAnonymousForUnauthenticatedAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("alice", "pwd"));
        when(repository.findByRequestKeyAndUsername("k1", "anonymous")).thenReturn(Optional.of(
                CartIdempotencyRecord.builder()
                        .requestKey("k1")
                        .username("anonymous")
                        .cartId(UUID.randomUUID())
                        .createdAt(LocalDateTime.now())
                        .build()));

        service.executeOrReplay("k1", () -> cartDTO(UUID.randomUUID()), this::cartDTO);

        verify(repository).findByRequestKeyAndUsername("k1", "anonymous");
    }

    @Test
    void executeOrReplayReplaysWinnerAfterConcurrentInsert() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("alice", "pwd", "ROLE_USER"));
        UUID cartId = UUID.randomUUID();
        var winner = CartIdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .requestKey("k1")
                .username("alice")
                .cartId(cartId)
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findByRequestKeyAndUsername("k1", "alice"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.saveAndFlush(any(CartIdempotencyRecord.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertEquals(cartId, service.executeOrReplay("k1", () -> cartDTO(UUID.randomUUID()), this::cartDTO).id());
    }

    @Test
    void executeOrReplayRethrowsConcurrentInsertWhenWinnerCannotBeFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("alice", "pwd", "ROLE_USER"));
        when(repository.findByRequestKeyAndUsername("k1", "alice")).thenReturn(Optional.empty());
        var exception = new DataIntegrityViolationException("duplicate");
        when(repository.saveAndFlush(any(CartIdempotencyRecord.class))).thenThrow(exception);
        when(repository.findByRequestKeyAndUsername("k1", "alice")).thenReturn(Optional.empty(), Optional.empty());

        assertEquals(exception, org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> service.executeOrReplay("k1", () -> cartDTO(UUID.randomUUID()), this::cartDTO)));
    }

    private CartDTO cartDTO(UUID id) {
        return new CartDTO(id, 1L, UUID.randomUUID(), BigDecimal.TEN, ReservationStatus.AWAITING_PAYMENT,
                LocalDateTime.now(), LocalDateTime.now(), List.of());
    }
}
