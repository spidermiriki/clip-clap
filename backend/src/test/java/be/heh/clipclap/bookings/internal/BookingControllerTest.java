package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class BookingControllerTest {

    static final String CLIENT_SUB = "sub-client@bookings-controller.test";
    static final String CREATOR_SUB = "sub-creator@bookings-controller.test";
    static final String TIERS_SUB = "sub-tiers@bookings-controller.test";

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @Autowired BookingServiceImpl bookingService;
    RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToApplicationContext(context)
                .configureServer(mockMvc -> mockMvc.apply(springSecurity()))
                .build();
    }

    @Test
    @WithMockUser(username = CLIENT_SUB)
    void detail_parLeClientAssocie_renvoieLaReservationEtSonHistorique() {
        long clientId = insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertUser("creator@bookings-controller.test", CREATOR_SUB, "CREATOR");
        long bookingId = bookingService.creer(clientId, creatorId).id();
        bookingService.changerStatut(bookingId, creatorId, "CONFIRMEE");

        client.get().uri("/api/bookings/{id}", bookingId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.booking.status").isEqualTo("CONFIRMEE")
                .jsonPath("$.historique.length()").isEqualTo(2)
                .jsonPath("$.historique[0].newStatus").isEqualTo("EN_ATTENTE")
                .jsonPath("$.historique[1].newStatus").isEqualTo("CONFIRMEE");
    }

    @Test
    @WithMockUser(username = CREATOR_SUB)
    void detail_parLeCreateurAssocie_estAutorise() {
        long clientId = insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertUser("creator@bookings-controller.test", CREATOR_SUB, "CREATOR");
        long bookingId = bookingService.creer(clientId, creatorId).id();

        client.get().uri("/api/bookings/{id}", bookingId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(username = TIERS_SUB)
    void detail_parUnTiers_renvoie403() {
        long clientId = insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertUser("creator@bookings-controller.test", CREATOR_SUB, "CREATOR");
        insertUser("tiers@bookings-controller.test", TIERS_SUB, "CLIENT");
        long bookingId = bookingService.creer(clientId, creatorId).id();

        client.get().uri("/api/bookings/{id}", bookingId)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void detail_sansConnexion_renvoie401() {
        client.get().uri("/api/bookings/{id}", 1)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(username = CLIENT_SUB)
    void detail_reservationInconnue_renvoie404() {
        insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");

        client.get().uri("/api/bookings/{id}", 999_999)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Réservation introuvable");
    }

    private long insertUser(String email, String sub, String role) {
        return jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, :role) RETURNING id")
                .param("email", email).param("sub", sub).param("role", role)
                .query(Long.class).single();
    }
}
