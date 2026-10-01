package com.alanensina.inventory.configurations;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

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
                config.kafkaListenerContainerFactory();
        assertInstanceOf(DefaultKafkaConsumerFactory.class, listenerFactory.getConsumerFactory());
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
