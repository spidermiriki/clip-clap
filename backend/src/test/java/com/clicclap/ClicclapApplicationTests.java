package com.clicclap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Démarre aussi compose.yaml pendant les tests (Docker doit être lancé).
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
class ClicclapApplicationTests {

	@Test
	void contextLoads() {
	}

}
