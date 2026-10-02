package com.alanensina.order.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaTopicInitializerConfigTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void skipsScriptWhenBothTopicsAreAlreadyInitialized() throws Exception {
        initializedCounts(1, 2);

        config().run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void initializesBothTopicsWhenNeitherIsRegistered() throws Exception {
        initializedCounts(0, 0);
        withProcessExitCode(0, () -> config().run(new DefaultApplicationArguments()));

        verify(jdbcTemplate).update(
                eq("INSERT INTO topic_initialization (topic_name, initialized_at) VALUES (?, ?)"),
                eq("orders"),
                any(java.sql.Timestamp.class));
        verify(jdbcTemplate).update(
                eq("INSERT INTO topic_initialization (topic_name, initialized_at) VALUES (?, ?)"),
                eq("notifications"),
                any(java.sql.Timestamp.class));
    }

    @Test
    void onlyRegistersTopicThatWasNotPreviouslyInitialized() throws Exception {
        initializedCounts(1, 0);
        withProcessExitCode(0, () -> config().run(new DefaultApplicationArguments()));

        verify(jdbcTemplate, never()).update(anyString(), eq("orders"), any(java.sql.Timestamp.class));
        verify(jdbcTemplate).update(anyString(), eq("notifications"), any(java.sql.Timestamp.class));
    }

    @Test
    void registersOrdersWhenOnlyNotificationTopicWasPreviouslyInitialized() throws Exception {
        initializedCounts(0, 1);
        withProcessExitCode(0, () -> config().run(new DefaultApplicationArguments()));

        verify(jdbcTemplate).update(anyString(), eq("orders"), any(java.sql.Timestamp.class));
        verify(jdbcTemplate, never()).update(anyString(), eq("notifications"), any(java.sql.Timestamp.class));
    }

    @Test
    void treatsNullInitializationCountsAsUninitialized() throws Exception {
        initializedCounts(null, null);
        withProcessExitCode(0, () -> config().run(new DefaultApplicationArguments()));

        verify(jdbcTemplate).update(anyString(), eq("orders"), any(java.sql.Timestamp.class));
        verify(jdbcTemplate).update(anyString(), eq("notifications"), any(java.sql.Timestamp.class));
    }

    @Test
    void reportsTopicScriptFailure() {
        initializedCounts(0, 0);

        assertThrows(IllegalStateException.class,
                () -> withProcessExitCode(1, () -> config().run(new DefaultApplicationArguments())));
    }

    private KafkaTopicInitializerConfig config() {
        return new KafkaTopicInitializerConfig(jdbcTemplate, "localhost:9094", "orders", "notifications");
    }

    private void initializedCounts(Integer orders, Integer notifications) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String topic = invocation.getArgument(2);
                    return "orders".equals(topic) ? orders : notifications;
                });
    }

    private void withProcessExitCode(int exitCode, ThrowingRunnable runnable) throws Exception {
        Process process = mock(Process.class);
        when(process.waitFor()).thenReturn(exitCode);
        try (var _ = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            when(builder.environment()).thenReturn(new HashMap<>());
            when(builder.inheritIO()).thenReturn(builder);
            when(builder.start()).thenReturn(process);
        })) {
            runnable.run();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
