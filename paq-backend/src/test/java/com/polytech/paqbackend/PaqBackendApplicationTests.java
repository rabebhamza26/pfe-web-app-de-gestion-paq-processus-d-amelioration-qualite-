package com.polytech.paqbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaqBackendApplicationTests {

    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
    }

    @Test
    void testApplicationStarts() {
        // Additional test to verify the application starts
        // This is just a placeholder for future tests
    }
}