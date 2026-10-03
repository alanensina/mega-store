package com.alanensina.inventory.repositories;

import com.alanensina.inventory.models.ProcessedOrderEvent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO PROCESSED_ORDER_EVENT (event_id, processed_at)
            VALUES (:eventId, CURRENT_TIMESTAMP)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int registerIfNotProcessed(@Param("eventId") UUID eventId);
}
