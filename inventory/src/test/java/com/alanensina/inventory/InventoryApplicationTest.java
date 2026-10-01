package com.alanensina.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class InventoryApplicationTest {

    @Test
    void mainStartsSpringApplication() {
        new InventoryApplication();
        try (var springApplication = mockStatic(SpringApplication.class)) {
            InventoryApplication.main(new String[]{"--spring.main.web-application-type=none"});

            springApplication.verify(() -> SpringApplication.run(
                    InventoryApplication.class, new String[]{"--spring.main.web-application-type=none"}));
        }
    }
}
