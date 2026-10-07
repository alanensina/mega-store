package com.alanensina.notification.configurations;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class KafkaConsumerConfigTest {

    @Test
    void listenerFactoryIsBuiltWithDeadLetterErrorHandler() {
        var factory = new KafkaConsumerConfig()
                .kafkaListenerContainerFactory(mock(KafkaTemplate.class));

        assertNotNull(factory.getContainerProperties());
        assertNotNull(new KafkaConsumerConfig().kafkaTemplate().getProducerFactory());
    }

    @Test
    void deadLetterDestinationAppendsDltSuffixAndLetsKafkaPickThePartition() {
        var failedRecord = new ConsumerRecord<>("orders_notification", 2, 0L, "k", "v");

        var destination = KafkaConsumerConfig.deadLetterDestination(failedRecord, new RuntimeException("boom"));

        assertEquals("orders_notification.DLT", destination.topic());
        assertEquals(-1, destination.partition());
    }
}
