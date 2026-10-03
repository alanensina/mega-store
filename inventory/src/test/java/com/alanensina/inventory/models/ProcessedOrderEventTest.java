package com.alanensina.inventory.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProcessedOrderEventTest {

    @Test
    void shouldCreateProcessedOrderEvent() {
        assertNotNull(new ProcessedOrderEvent());
    }
}
