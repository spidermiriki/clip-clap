package be.heh.clipclap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ActuatorHealthIT {

    @Autowired
    RestTestClient client;

    @Test
    void actuatorHealth_sansAuthentification_renvoie200() {
        client.get()
              .uri("/actuator/health")
              .exchange()
              .assertThat()
              .hasStatusOk();
    }
}
