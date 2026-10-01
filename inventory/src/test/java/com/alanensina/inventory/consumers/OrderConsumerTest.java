package com.alanensina.inventory.consumers;

import com.alanensina.inventory.configurations.JacksonConfig;
import com.alanensina.inventory.dtos.OrderEventDTO;
import com.alanensina.inventory.dtos.ReservationItemEvent;
import com.alanensina.inventory.enums.OrderEventType;
import com.alanensina.inventory.enums.ReservationStatus;
import com.alanensina.inventory.exceptions.KafkaErrorException;
import com.alanensina.inventory.services.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderConsumerTest {

    private ReservationService reservationService;
    private OrderConsumer consumer;
    private UUID cartId;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        consumer = new OrderConsumer(reservationService, new JacksonConfig().objectMapper());
        cartId = UUID.randomUUID();
    }

    @Test
    void consumeDispatchesCreateUpdateAndCancelEvents() throws Exception {
        consumer.consume(message(OrderEventType.CREATE));
        consumer.consume(message(OrderEventType.UPDATE));
        consumer.consume(message(OrderEventType.CANCEL));

        verify(reservationService).create(any(OrderEventDTO.class));
        verify(reservationService).update(any(OrderEventDTO.class));
        verify(reservationService).cancel(any(OrderEventDTO.class));
    }

    @Test
    void consumeWrapsInvalidJson() {
        assertThrows(KafkaErrorException.class, () -> consumer.consume("{"));
        verifyNoInteractions(reservationService);
    }

    @Test
    void consumeWrapsErrorsFromReservationService() {
        doThrow(new IllegalStateException("service failed"))
                .when(reservationService).create(any(OrderEventDTO.class));

        KafkaErrorException exception = assertThrows(KafkaErrorException.class,
                () -> consumer.consume(message(OrderEventType.CREATE)));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("service failed"));
    }

    @Test
    void consumeWrapsEventWithoutType() throws Exception {
        String message = new JacksonConfig().objectMapper().writeValueAsString(
                new OrderEventDTO(null, cartId, List.of(), ReservationStatus.PAID,
                        LocalDateTime.now(), LocalDateTime.now()));

        assertThrows(KafkaErrorException.class, () -> consumer.consume(message));
        verifyNoInteractions(reservationService);
    }

    private String message(OrderEventType type) throws Exception {
        return new JacksonConfig().objectMapper().writeValueAsString(
                new OrderEventDTO(type, cartId,
                        List.of(new ReservationItemEvent(UUID.randomUUID(), 1)),
                        ReservationStatus.PAID, LocalDateTime.now(), LocalDateTime.now()));
    }
}
