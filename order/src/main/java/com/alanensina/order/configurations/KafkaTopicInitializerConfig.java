package com.alanensina.order.configurations;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicInitializerConfig implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;
	private final String bootstrapServers;
	private final String ordersTopic;
	private final String notificationTopic;

	public KafkaTopicInitializerConfig(
			JdbcTemplate jdbcTemplate,
			@Value("${spring.kafka.bootstrap-servers:localhost:9094}") String bootstrapServers,
			@Value("${spring.kafka.orders.topic.name:orders_topic}") String ordersTopic,
			@Value("${spring.kafka.orders.notification.topic.name:orders_notification}") String notificationTopic) {
		this.jdbcTemplate = jdbcTemplate;
		this.bootstrapServers = bootstrapServers;
		this.ordersTopic = ordersTopic;
		this.notificationTopic = notificationTopic;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		boolean ordersInitialized = isInitialized(ordersTopic);
		boolean notificationInitialized = isInitialized(notificationTopic);
		if (ordersInitialized && notificationInitialized) {
			return;
		}

		executeScript();

		Timestamp now = Timestamp.from(Instant.now());
		if (!ordersInitialized) {
			jdbcTemplate.update(
					"INSERT INTO topic_initialization (topic_name, initialized_at) VALUES (?, ?)",
					ordersTopic,
					now);
		}
		if (!notificationInitialized) {
			jdbcTemplate.update(
					"INSERT INTO topic_initialization (topic_name, initialized_at) VALUES (?, ?)",
					notificationTopic,
					now);
		}
	}

	private boolean isInitialized(String topic) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM topic_initialization WHERE topic_name = ?",
				Integer.class,
				topic);
		return count != null && count > 0;
	}

	private void executeScript() throws IOException, InterruptedException {
		ClassPathResource resource = new ClassPathResource("create_topics.sh");
		String script = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		Process process = createProcess(script);
		if (process.waitFor() != 0) {
			throw new IllegalStateException("Kafka topic creation script failed");
		}
	}

	private Process createProcess(String script) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", script);
		processBuilder.environment().put("KAFKA_BOOTSTRAP_SERVERS", bootstrapServers);
		// export comma-separated topics for the script
		processBuilder.environment().put("KAFKA_TOPICS", ordersTopic + "," + notificationTopic);
		processBuilder.inheritIO();
		return processBuilder.start();
	}
}
