package be.heh.clipclap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ActuatorHealthIT {

    @Autowired
    WebTestClient client;

    @Test
    void actuatorHealth_sansAuthentification_renvoie200() {
        client.get()
              .uri("/actuator/health")
              .exchange()
              .expectStatus().isOk();
    }
}
