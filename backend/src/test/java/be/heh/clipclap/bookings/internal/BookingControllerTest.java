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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// ALL_DEPENDENCIES : bookings dépend d'offerings, qui dépend lui-même de creators (CreatorProfileService,
// nécessaire pour valider le profil du créateur) — DIRECT_DEPENDENCIES ne suit pas cette transitivité.
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
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
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long offeringId = insertOffering(creatorId, 60, "100.00");
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        long bookingId = bookingService.creer(clientId, new BookingCommand(offeringId, slotId, java.util.Set.of())).id();
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
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long offeringId = insertOffering(creatorId, 60, "100.00");
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        long bookingId = bookingService.creer(clientId, new BookingCommand(offeringId, slotId, java.util.Set.of())).id();

        client.get().uri("/api/bookings/{id}", bookingId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(username = TIERS_SUB)
    void detail_parUnTiers_renvoie403() {
        long clientId = insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        insertUser("tiers@bookings-controller.test", TIERS_SUB, "CLIENT");
        long offeringId = insertOffering(creatorId, 60, "100.00");
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        long bookingId = bookingService.creer(clientId, new BookingCommand(offeringId, slotId, java.util.Set.of())).id();

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

    @Test
    @WithMockUser(username = CLIENT_SUB)
    void creer_prestationEtCreneauValides_renvoie201AvecLeTotal() {
        insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long offeringId = insertOffering(creatorId, 60, "100.00");
        long optionId = insertOption(offeringId, "20.00");
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        client.post().uri("/api/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new BookingCommand(offeringId, slotId, java.util.Set.of(optionId)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("EN_ATTENTE")
                .jsonPath("$.totalPrice").isEqualTo(120.00);
    }

    @Test
    @WithMockUser(username = CREATOR_SUB)
    void creer_createurReserveSaProprePrestation_renvoie403() {
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long offeringId = insertOffering(creatorId, 60, "100.00");
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        client.post().uri("/api/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new BookingCommand(offeringId, slotId, java.util.Set.of()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = CLIENT_SUB)
    void creer_creneauTropCourt_renvoie409() {
        insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long offeringId = insertOffering(creatorId, 90, "100.00");
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T09:30+00:00");

        client.post().uri("/api/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new BookingCommand(offeringId, slotId, java.util.Set.of()))
                .exchange()
                .expectStatus().isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
    }

    @Test
    @WithMockUser(username = CLIENT_SUB)
    void creer_slotIdManquant_renvoie400() {
        insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long offeringId = insertOffering(creatorId, 60, "100.00");

        client.post().uri("/api/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body("{\"offeringId\":" + offeringId + "}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = CLIENT_SUB)
    void creer_optionInconnue_renvoie400() {
        insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long offeringId = insertOffering(creatorId, 60, "100.00");
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        client.post().uri("/api/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new BookingCommand(offeringId, slotId, java.util.Set.of(999_999L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = CLIENT_SUB)
    void creer_prestationInconnue_renvoie404() {
        insertUser("client@bookings-controller.test", CLIENT_SUB, "CLIENT");
        long creatorId = insertCreator("creator@bookings-controller.test", CREATOR_SUB);
        long slotId = insertSlot(creatorId, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        client.post().uri("/api/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new BookingCommand(999_999L, slotId, java.util.Set.of()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void creer_sansConnexion_renvoie401() {
        client.post().uri("/api/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new BookingCommand(1L, 1L, java.util.Set.of()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private long insertUser(String email, String sub, String role) {
        return jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, :role) RETURNING id")
                .param("email", email).param("sub", sub).param("role", role)
                .query(Long.class).single();
    }

    private long insertCreator(String email, String sub) {
        long id = insertUser(email, sub, "CREATOR");
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name, validation_status) VALUES (:id, :n, 'VALIDATED')")
                .param("id", id).param("n", email)
                .update();
        return id;
    }

    private long insertOffering(long creatorId, int durationMinutes, String basePrice) {
        return jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                VALUES (:c, 'Prestation', :d, :p) RETURNING id
                """)
                .param("c", creatorId).param("d", durationMinutes).param("p", new BigDecimal(basePrice))
                .query(Long.class).single();
    }

    private long insertOption(long offeringId, String extraPrice) {
        return jdbc.sql("""
                INSERT INTO offering_options (offering_id, name, extra_price)
                VALUES (:o, 'Option', :p) RETURNING id
                """)
                .param("o", offeringId).param("p", new BigDecimal(extraPrice))
                .query(Long.class).single();
    }

    private long insertSlot(long creatorId, String startsAt, String endsAt) {
        return jdbc.sql("""
                INSERT INTO availability_slots (creator_id, starts_at, ends_at)
                VALUES (:c, :d, :f) RETURNING id
                """)
                .param("c", creatorId)
                .param("d", OffsetDateTime.parse(startsAt))
                .param("f", OffsetDateTime.parse(endsAt))
                .query(Long.class).single();
    }
}
