package com.alanensina.customer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CustomerApplicationTests {

    @Test
    void applicationClassShouldLoad() {
        assertDoesNotThrow(() -> Class.forName(CustomerApplication.class.getName()));
    }
}
