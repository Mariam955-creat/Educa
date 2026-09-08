package com.educa.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie que le contexte Spring démarre et que les migrations Flyway passent
 * sur la base {@code educa_test} (profil {@code test}).
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
