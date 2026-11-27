package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.offerings.AvailableSlot;
import be.heh.clipclap.offerings.BookedPeriod;
import be.heh.clipclap.offerings.BookedPeriodsProvider;
import be.heh.clipclap.offerings.SlotUsageChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// Horloge figée au 25 mars 2027 ; BookedPeriodsProvider et SlotUsageChecker simulés (fournis par bookings)
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class AvailabilityControllerTest {

    static final String ALICE = "sub-alice@slots.test";
    static final Clock HORLOGE = Clock.fixed(Instant.parse("2027-03-25T10:00:00Z"), ZoneOffset.UTC);
    static final String VENDREDI_14H_16H = "{\"start\":\"2027-04-02T14:00\",\"end\":\"2027-04-02T16:00\"}";

    @TestBean Clock offeringsClock;
    @MockitoBean BookedPeriodsProvider bookedPeriods;
    @MockitoBean SlotUsageChecker slotUsage;
    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    RestTestClient client;

    static Clock offeringsClock() {
        return HORLOGE;
    }

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToApplicationContext(context)
                .configureServer(mockMvc -> mockMvc.apply(springSecurity()))
                .build();
    }

    // ── Créneaux du créateur (scénario 12) ──────────────────────────────────

    @Test
    @WithMockUser(username = ALICE)
    void ajouterPuisLister_aLHeureDeBruxelles() {
        insertCreator("alice@slots.test", ALICE);

        client.post().uri("/api/offerings/me/slots")
                .contentType(MediaType.APPLICATION_JSON).body(VENDREDI_14H_16H)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.start").isEqualTo("2027-04-02T14:00:00+02:00")
                .jsonPath("$.end").isEqualTo("2027-04-02T16:00:00+02:00");

        client.get().uri("/api/offerings/me/slots?from=2027-04-01&to=2027-04-30")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    @WithMockUser(username = ALICE)
    void chevauchement_renvoie409_maisSeToucherEstPermis() {
        insertCreator("alice@slots.test", ALICE);
        create(VENDREDI_14H_16H);

        client.post().uri("/api/offerings/me/slots")
                .contentType(MediaType.APPLICATION_JSON).body("{\"start\":\"2027-04-02T15:00\",\"end\":\"2027-04-02T17:00\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.title").isEqualTo("Créneau en conflit");
        client.post().uri("/api/offerings/me/slots")
                .contentType(MediaType.APPLICATION_JSON).body("{\"start\":\"2027-04-02T16:00\",\"end\":\"2027-04-02T17:00\"}")
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @WithMockUser(username = ALICE)
    void creneauxInvalides_renvoient400() {
        insertCreator("alice@slots.test", ALICE);

        for (String body : new String[] {
                "{\"start\":\"2027-04-02T16:00\",\"end\":\"2027-04-02T14:00\"}",   // fin avant début
                "{\"start\":\"2027-03-20T14:00\",\"end\":\"2027-03-20T16:00\"}",   // passé
                "{\"start\":\"2027-04-02T08:00\",\"end\":\"2027-04-03T09:00\"}",   // plus de 24 h
                "{\"start\":\"2027-04-02T08:00\"}",
        }) {
            client.post().uri("/api/offerings/me/slots")
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.title").isEqualTo("Disponibilité invalide");
        }
    }

    @Test
    @WithMockUser(username = ALICE)
    void supprimer_sonCreneau_204_reserve_409_dUnAutre_404() {
        insertCreator("alice@slots.test", ALICE);
        long bob = insertCreator("bob@slots.test", "sub-bob@slots.test");
        long libre = create(VENDREDI_14H_16H);
        long reserve = create("{\"start\":\"2027-04-03T14:00\",\"end\":\"2027-04-03T16:00\"}");
        long deBob = jdbc.sql("""
                INSERT INTO availability_slots (creator_id, starts_at, ends_at)
                VALUES (:bob, '2027-04-02T12:00:00Z', '2027-04-02T13:00:00Z') RETURNING id
                """).param("bob", bob).query(Long.class).single();
        // Réservation En attente ou Confirmée (#136)
        when(slotUsage.hasActiveBookings(reserve)).thenReturn(true);

        client.delete().uri("/api/offerings/me/slots/{id}", libre).exchange().expectStatus().isNoContent();
        client.delete().uri("/api/offerings/me/slots/{id}", reserve)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isEqualTo("Ce créneau est réservé : il ne peut pas être supprimé");
        client.delete().uri("/api/offerings/me/slots/{id}", deBob).exchange().expectStatus().isNotFound();

        assertThat(slotExiste(libre)).isFalse();
        assertThat(slotExiste(reserve)).isTrue();
    }

    @Test
    @WithMockUser(username = ALICE)
    void supprimer_creneauSansReservationActive_204_memeSiUnePeriodeConfirmeeLeChevauche() {
        // Seul SlotUsageChecker décide de la suppression ; BookedPeriodsProvider ne sert qu'au filtrage
        insertCreator("alice@slots.test", ALICE);
        long slot = create(VENDREDI_14H_16H);
        when(bookedPeriods.findBookedPeriods(anyLong(), any(), any())).thenReturn(List.of(
                new BookedPeriod(OffsetDateTime.parse("2027-04-02T14:00+02:00"), OffsetDateTime.parse("2027-04-02T16:00+02:00"))));

        client.delete().uri("/api/offerings/me/slots/{id}", slot).exchange().expectStatus().isNoContent();

        assertThat(slotExiste(slot)).isFalse();
    }

    // ── Préavis et marge (scénario 13) ──────────────────────────────────────

    @Test
    @WithMockUser(username = ALICE)
    void reglages_parDefautZero_puisEnregistres_etBornes() {
        insertCreator("alice@slots.test", ALICE);

        client.get().uri("/api/offerings/me/settings")
                .exchange()
                .expectBody()
                .jsonPath("$.minNoticeHours").isEqualTo(0)
                .jsonPath("$.bufferMinutes").isEqualTo(0);
        client.put().uri("/api/offerings/me/settings")
                .contentType(MediaType.APPLICATION_JSON).body("{\"minNoticeHours\":48,\"bufferMinutes\":30}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.minNoticeHours").isEqualTo(48);
        client.put().uri("/api/offerings/me/settings")
                .contentType(MediaType.APPLICATION_JSON).body("{\"minNoticeHours\":999,\"bufferMinutes\":30}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── Client : créneaux sélectionnables ───────────────────────────────────

    @Test
    @WithMockUser(username = "sub-client@slots.test")
    void client_voitLesCreneauxAssezLongsPourLaPrestation() {
        long alice = insertValidatedCreator("alice@slots.test", ALICE);
        jdbc.sql("""
                INSERT INTO availability_slots (creator_id, starts_at, ends_at) VALUES
                    (:a, '2027-04-02T14:00+02:00', '2027-04-02T15:00+02:00'),
                    (:a, '2027-04-02T16:00+02:00', '2027-04-02T19:00+02:00')
                """).param("a", alice).update();
        long troisHeures = insertOffering(alice, 180);

        client.get().uri("/api/offerings/creators/{id}/slots?date=2027-04-02", alice)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);
        client.get().uri("/api/offerings/creators/{id}/slots?date=2027-04-02&offeringId={o}", alice, troisHeures)
                .exchange()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].start").isEqualTo("2027-04-02T16:00:00+02:00");
        client.get().uri("/api/offerings/creators/{id}/slots?date=2027-04-02&offeringId=999999", alice)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── Accès ───────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "sub-sans-profil@slots.test")
    void sansProfilCreateur_renvoie404() {
        insertUser("sans-profil@slots.test", "sub-sans-profil@slots.test");

        client.post().uri("/api/offerings/me/slots")
                .contentType(MediaType.APPLICATION_JSON).body(VENDREDI_14H_16H)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Profil introuvable");
    }

    @Test
    void sansConnexion_renvoie401() {
        client.get().uri("/api/offerings/me/slots").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/offerings/creators/1/slots?date=2027-04-02").exchange().expectStatus().isUnauthorized();
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    private long create(String body) {
        AvailableSlot slot = client.post().uri("/api/offerings/me/slots")
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AvailableSlot.class)
                .returnResult().getResponseBody();
        assertThat(slot).isNotNull();
        return slot.id();
    }

    private long insertOffering(long creatorId, int dureeMinutes) {
        return jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                VALUES (:c, 'Prestation', :d, 100) RETURNING id
                """)
                .param("c", creatorId).param("d", dureeMinutes)
                .query(Long.class).single();
    }

    private long insertUser(String email, String sub) {
        return jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, 'CREATOR') RETURNING id")
                .param("email", email).param("sub", sub)
                .query(Long.class).single();
    }

    private long insertCreator(String email, String sub) {
        long id = insertUser(email, sub);
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name) VALUES (:id, :name)")
                .param("id", id).param("name", email)
                .update();
        return id;
    }

    private long insertValidatedCreator(String email, String sub) {
        long id = insertCreator(email, sub);
        jdbc.sql("UPDATE creator_profiles SET validation_status = 'VALIDATED' WHERE user_id = :id").param("id", id).update();
        return id;
    }

    private boolean slotExiste(long slotId) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM availability_slots WHERE id = :id)")
                .param("id", slotId).query(Boolean.class).single();
    }
}
