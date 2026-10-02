package com.alanensina.order.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicEventTypeTest {

    @Test
    void exposesTopicEventTypes() {
        assertArrayEquals(new TopicEventType[]{TopicEventType.ORDER, TopicEventType.NOTIFICATION},
                TopicEventType.values());
        assertEquals(TopicEventType.ORDER, TopicEventType.valueOf("ORDER"));
    }
}
