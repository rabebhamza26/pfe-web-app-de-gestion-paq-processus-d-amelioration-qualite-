package com.polytech.paqbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")           // ← ajoute cette ligne

class PaqBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}


