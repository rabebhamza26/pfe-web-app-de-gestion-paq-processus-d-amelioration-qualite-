package com.polytech.paqbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // ← ONLY use this, no @TestPropertySource
class PaqBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}