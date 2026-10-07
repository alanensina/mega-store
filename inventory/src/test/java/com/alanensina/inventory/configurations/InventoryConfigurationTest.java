package com.alanensina.inventory.configurations;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class InventoryConfigurationTest {

    @Test
    void jacksonConfigRegistersJavaTimeAndUsesIsoDateStrings() throws Exception {
        var mapper = new JacksonConfig().objectMapper();

        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        assertEquals("\"2024-01-02T10:30:00\"",
                mapper.writeValueAsString(java.time.LocalDateTime.of(2024, 1, 2, 10, 30)));
        assertEquals(java.time.LocalDateTime.of(2024, 1, 2, 10, 30),
                mapper.readValue("\"2024-01-02T10:30:00\"", java.time.LocalDateTime.class));
    }

    @Test
    void kafkaConfigCreatesConsumerFactoryWithExpectedSettings() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        var consumerFactory = config.consumerFactory();
        var properties = ((DefaultKafkaConsumerFactory<String, String>) consumerFactory)
                .getConfigurationProperties();

        assertEquals("localhost:9094", properties.get("bootstrap.servers"));
        assertEquals("inventory", properties.get("group.id"));
        assertEquals("earliest", properties.get("auto.offset.reset"));

        ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory =
                config.kafkaListenerContainerFactory(mock(KafkaTemplate.class));
        assertInstanceOf(DefaultKafkaConsumerFactory.class, listenerFactory.getConsumerFactory());
        assertNotNull(config.kafkaTemplate().getProducerFactory());
    }

    @Test
    void deadLetterDestinationAppendsDltSuffixAndLetsKafkaPickThePartition() {
        var failedRecord = new org.apache.kafka.clients.consumer.ConsumerRecord<>("orders_topic", 3, 0L, "k", "v");

        var destination = KafkaConsumerConfig.deadLetterDestination(failedRecord, new RuntimeException("boom"));

        assertEquals("orders_topic.DLT", destination.topic());
        assertEquals(-1, destination.partition());
    }

    @Test
    void swaggerConfigBuildsBearerSecuredApiDescription() {
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "title", "inventory-api");
        ReflectionTestUtils.setField(config, "version", "1.0");

        var openApi = config.customOpenAPI();

        assertEquals("inventory-api", openApi.getInfo().getTitle());
        assertEquals("1.0", openApi.getInfo().getVersion());
        assertEquals("A simple API to manage inventory.", openApi.getInfo().getDescription());
        assertEquals("bearer", openApi.getComponents().getSecuritySchemes().get("bearerAuth").getScheme());
        assertEquals("JWT", openApi.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat());
        assertTrue(openApi.getSecurity().getFirst().containsKey("bearerAuth"));
    }
}
