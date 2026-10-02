package com.alanensina.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class OrderApplicationTest {

    @Test
    void delegatesToSpringApplication() {
        String[] args = {"--spring.main.web-application-type=none"};
        try (var springApplication = mockStatic(SpringApplication.class)) {
            OrderApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(OrderApplication.class, args));
        }
    }
}
