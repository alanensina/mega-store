package com.alanensina.notification;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class NotificationApplicationTest {

    @Test
    void mainShouldStartSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            NotificationApplication.main(new String[]{"--test"});

            springApplication.verify(() ->
                    SpringApplication.run(eq(NotificationApplication.class), eq(new String[]{"--test"})));
        }
    }
}
