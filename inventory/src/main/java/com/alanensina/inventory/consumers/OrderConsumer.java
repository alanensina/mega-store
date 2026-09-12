package com.alanensina.inventory.consumers;

import com.alanensina.inventory.dtos.CreateReservationDTO;
import com.alanensina.inventory.dtos.OrderEventDTO;
import com.alanensina.inventory.dtos.ReservationItemDTO;
import com.alanensina.inventory.exceptions.InvalidKafkaOrderEventTypeException;
import com.alanensina.inventory.services.ReservationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class OrderConsumer {

    private final ReservationService reservationService;

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(OrderEventDTO event){

        switch(event.eventType()){

            case CREATE:
                reservationService.create(new CreateReservationDTO(
                        event.cartId(),
                        event.items().stream()
                                .map(item -> new ReservationItemDTO(item.productId(), item.quantity()))
                                .toList(),
                        event.status(),
                        event.createdAt(),
                        event.updatedAt()
                ));
                break;

//            case UPDATE:
//                break;

//            case CANCEL:
//                break;

            default:
                log.error("Unhandled event type: {}", event.eventType());
                throw new InvalidKafkaOrderEventTypeException(event.eventType().name());
        }
    }
}
