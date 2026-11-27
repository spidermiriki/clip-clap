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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Suppression d'un créneau par son créateur (#136), avec de vraies réservations : bookings fournit
 * {@link be.heh.clipclap.offerings.SlotUsageChecker} à offerings.
 */
// ALL_DEPENDENCIES : l'endpoint de suppression vit dans offerings, qui dépend de creators
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class BookingSlotUsageCheckerTest {

    static final String CREATOR_SUB = "sub-creator@slot-usage.test";

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    RestTestClient client;

    long clientId;
    long creatorId;
    long offeringId;
    long slotId;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToApplicationContext(context)
                .configureServer(mockMvc -> mockMvc.apply(springSecurity()))
                .build();
        clientId = insertUser("client@slot-usage.test", "sub-client@slot-usage.test", "CLIENT");
        creatorId = insertUser("creator@slot-usage.test", CREATOR_SUB, "CREATOR");
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name, validation_status) VALUES (:id, 'Créateur', 'VALIDATED')")
                .param("id", creatorId).update();
        offeringId = jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                VALUES (:c, 'Reportage', 60, 300) RETURNING id
                """).param("c", creatorId).query(Long.class).single();
        slotId = jdbc.sql("""
                INSERT INTO availability_slots (creator_id, starts_at, ends_at)
                VALUES (:c, :d, :f) RETURNING id
                """)
                .param("c", creatorId)
                .param("d", OffsetDateTime.parse("2030-01-01T09:00+00:00"))
                .param("f", OffsetDateTime.parse("2030-01-01T10:00+00:00"))
                .query(Long.class).single();
    }

    @Test
    @WithMockUser(username = CREATOR_SUB)
    void supprimer_creneauAvecReservationEnAttente_renvoie409EtLeGarde() {
        insertBooking("EN_ATTENTE");

        client.delete().uri("/api/offerings/me/slots/{id}", slotId)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isEqualTo("Ce créneau est réservé : il ne peut pas être supprimé");

        assertThat(slotExiste()).isTrue();
    }

    @Test
    @WithMockUser(username = CREATOR_SUB)
    void supprimer_creneauAvecReservationConfirmee_renvoie409EtLeGarde() {
        insertBooking("CONFIRMEE");

        client.delete().uri("/api/offerings/me/slots/{id}", slotId)
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(slotExiste()).isTrue();
    }

    @Test
    @WithMockUser(username = CREATOR_SUB)
    void supprimer_creneauAvecSeulementUneReservationRefusee_renvoie204EtVideSlotId() {
        long booking = insertBooking("REFUSEE");

        client.delete().uri("/api/offerings/me/slots/{id}", slotId)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(slotExiste()).isFalse();
        var restante = jdbc.sql("SELECT slot_id, status FROM bookings WHERE id = :id")
                .param("id", booking)
                .query((rs, rowNum) -> new Object[] {rs.getObject("slot_id"), rs.getString("status")})
                .single();
        assertThat(restante[0]).isNull();
        assertThat(restante[1]).isEqualTo("REFUSEE");
    }

    private long insertBooking(String status) {
        return jdbc.sql("""
                INSERT INTO bookings (client_id, creator_id, offering_id, slot_id, total_price, status)
                VALUES (:client, :creator, :offering, :slot, 300, :status) RETURNING id
                """)
                .param("client", clientId).param("creator", creatorId)
                .param("offering", offeringId).param("slot", slotId).param("status", status)
                .query(Long.class).single();
    }

    private boolean slotExiste() {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM availability_slots WHERE id = :id)")
                .param("id", slotId).query(Boolean.class).single();
    }

    private long insertUser(String email, String sub, String role) {
        return jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, :role) RETURNING id")
                .param("email", email).param("sub", sub).param("role", role)
                .query(Long.class).single();
    }
}
