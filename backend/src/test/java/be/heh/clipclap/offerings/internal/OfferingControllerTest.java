package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.offerings.Offering;
import be.heh.clipclap.offerings.OfferingUsageChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// bookings ne fournit pas encore d'OfferingUsageChecker : un mock le remplace (faux par défaut)
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class OfferingControllerTest {

    static final String ALICE = "sub-alice@offerings.test";
    static final String BOB = "sub-bob@offerings.test";
    static final String MARIAGE = """
            {"name":"  Reportage mariage  ","durationMinutes":480,"basePrice":1200,
             "options":[{"name":"Drone","extraPrice":150},{"name":"Album photo","extraPrice":89.90}]}
            """;

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @MockitoBean OfferingUsageChecker usageChecker;
    RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToApplicationContext(context)
                .configureServer(mockMvc -> mockMvc.apply(springSecurity()))
                .build();
    }

    // ── Scénarios 10 et 11 : créer une prestation et ses options ────────────

    @Test
    @WithMockUser(username = ALICE)
    void creerPuisLister_avecSesOptions() {
        insertCreator("alice@offerings.test", ALICE);

        client.post().uri("/api/offerings/me")
                .contentType(MediaType.APPLICATION_JSON).body(MARIAGE)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.name").isEqualTo("Reportage mariage")
                .jsonPath("$.options.length()").isEqualTo(2);

        client.get().uri("/api/offerings/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].durationMinutes").isEqualTo(480)
                .jsonPath("$[0].basePrice").isEqualTo(1200.0)
                .jsonPath("$[0].options[0].name").isEqualTo("Drone")
                .jsonPath("$[0].options[1].extraPrice").isEqualTo(89.9);
    }

    @Test
    @WithMockUser(username = ALICE)
    void modifier_remplaceLesChampsEtLesOptions() {
        insertCreator("alice@offerings.test", ALICE);
        long id = create(MARIAGE);

        client.put().uri("/api/offerings/me/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Mariage demi-journée\",\"durationMinutes\":240,\"basePrice\":650,"
                        + "\"options\":[{\"name\":\"Retouches\",\"extraPrice\":40}]}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Mariage demi-journée")
                .jsonPath("$.durationMinutes").isEqualTo(240)
                .jsonPath("$.options.length()").isEqualTo(1)
                .jsonPath("$.options[0].name").isEqualTo("Retouches");
    }

    @Test
    @WithMockUser(username = ALICE)
    void supprimer_renvoie204_etLaPrestationDisparait() {
        insertCreator("alice@offerings.test", ALICE);
        long id = create(MARIAGE);

        client.delete().uri("/api/offerings/me/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/offerings/me")
                .exchange()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    // ── Scénario 40 : suppression refusée si une réservation est liée ───────

    @Test
    @WithMockUser(username = ALICE)
    void supprimer_avecReservationActive_renvoie409() {
        insertCreator("alice@offerings.test", ALICE);
        long id = create(MARIAGE);
        when(usageChecker.hasActiveBookings(id)).thenReturn(true);

        client.delete().uri("/api/offerings/me/{id}", id)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.title").isEqualTo("Prestation réservée");

        client.get().uri("/api/offerings/me")
                .exchange()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    // ── Validation ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = ALICE)
    void donneesInvalides_renvoient400ProblemDetail() {
        insertCreator("alice@offerings.test", ALICE);

        for (String body : new String[] {
                "{\"name\":\" \",\"durationMinutes\":60,\"basePrice\":100}",
                "{\"name\":\"Portrait\",\"durationMinutes\":0,\"basePrice\":100}",
                "{\"name\":\"Portrait\",\"durationMinutes\":60,\"basePrice\":-1}",
                "{\"name\":\"Portrait\",\"durationMinutes\":60,\"basePrice\":10.555}",
                "{\"name\":\"Portrait\",\"durationMinutes\":60}",
                "{\"name\":\"Portrait\",\"durationMinutes\":60,\"basePrice\":100,\"options\":[{\"name\":\"\",\"extraPrice\":10}]}",
                "{\"name\":\"Portrait\",\"durationMinutes\":60,\"basePrice\":100,\"options\":[{\"name\":\"Drone\",\"extraPrice\":-5}]}",
        }) {
            client.post().uri("/api/offerings/me")
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.title").isEqualTo("Prestation invalide");
        }
    }

    // ── Accès ───────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = BOB)
    void prestationDUnAutreCreateur_renvoie404() {
        insertCreator("alice@offerings.test", ALICE);
        insertCreator("bob@offerings.test", BOB);
        long deAlice = jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                SELECT id, 'Portrait', 60, 100 FROM users WHERE sub = :sub
                RETURNING id
                """).param("sub", ALICE).query(Long.class).single();

        client.put().uri("/api/offerings/me/{id}", deAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Volée\",\"durationMinutes\":60,\"basePrice\":1}")
                .exchange()
                .expectStatus().isNotFound();
        client.delete().uri("/api/offerings/me/{id}", deAlice)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @WithMockUser(username = "sub-sans-profil@offerings.test")
    void sansProfilCreateur_renvoie404ProfilIntrouvable() {
        insertUser("sans-profil@offerings.test", "sub-sans-profil@offerings.test");

        client.post().uri("/api/offerings/me")
                .contentType(MediaType.APPLICATION_JSON).body(MARIAGE)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Profil introuvable");
    }

    @Test
    void sansConnexion_renvoie401() {
        client.get().uri("/api/offerings/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    private long create(String body) {
        Offering created = client.post().uri("/api/offerings/me")
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Offering.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        return created.id();
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
}
