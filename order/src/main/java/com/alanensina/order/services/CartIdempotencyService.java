package com.alanensina.order.services;

import com.alanensina.order.dtos.CartDTO;
import com.alanensina.order.exceptions.IdempotencyRequestInProgressException;
import com.alanensina.order.models.CartIdempotencyRecord;
import com.alanensina.order.repositories.CartIdempotencyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class CartIdempotencyService {

    private final CartIdempotencyRepository repository;

    public CartIdempotencyService(CartIdempotencyRepository repository) {
        this.repository = repository;
    }

    public CartDTO executeOrReplay(
            String requestKey,
            Supplier<CartDTO> createAction,
            Function<UUID, CartDTO> replayAction
    ) {
        var normalizedKey = normalizeKey(requestKey);
        var username = currentUsername();

        var existingRecord = repository.findByRequestKeyAndUsername(normalizedKey, username);
        if (existingRecord.isPresent()) {
            return replay(existingRecord.get(), replayAction);
        }

        var newRecord = CartIdempotencyRecord.builder()
                .requestKey(normalizedKey)
                .username(username)
                .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                .build();

        try {
            newRecord = repository.saveAndFlush(newRecord);
        } catch (DataIntegrityViolationException ex) {
            var winner = repository.findByRequestKeyAndUsername(normalizedKey, username)
                    .orElseThrow(() -> ex);
            return replay(winner, replayAction);
        }

        try {
            var createdCart = createAction.get();
            newRecord.setCartId(createdCart.id());
            repository.save(newRecord);
            return createdCart;
        } catch (RuntimeException ex) {
            repository.deleteById(newRecord.getId());
            throw ex;
        }
    }

    private static String normalizeKey(String requestKey) {
        var trimmed = Objects.requireNonNullElse(requestKey, "").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Idempotency key must not be blank.");
        }
        return trimmed;
    }

    private static CartDTO replay(CartIdempotencyRecord replayedRecord, Function<UUID, CartDTO> replayAction) {
        if (replayedRecord.getCartId() == null) {
            throw new IdempotencyRequestInProgressException(replayedRecord.getRequestKey());
        }
        return replayAction.apply(replayedRecord.getCartId());
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
