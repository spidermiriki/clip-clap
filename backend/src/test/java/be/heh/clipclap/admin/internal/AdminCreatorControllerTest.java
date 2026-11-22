package be.heh.clipclap.admin.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// ALL_DEPENDENCIES : la règle « /api/admin/** réservé à ADMIN » vient de la sécurité du module users,
// dont admin ne dépend qu'à travers creators
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminCreatorControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @Autowired CreatorProfileService creatorProfileService;
    RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToApplicationContext(context)
                .configureServer(mockMvc -> mockMvc.apply(springSecurity()))
                .build();
    }

    // ── Accès ───────────────────────────────────────────────────────────────

    @Test
    void sansConnexion_renvoie401() {
        client.get().uri("/api/admin/creators").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(roles = "CREATOR")
    void createur_renvoie403() {
        long enAttente = insertCreator("attente@admin.test", "PENDING");

        client.get().uri("/api/admin/creators").exchange().expectStatus().isForbidden();
        client.post().uri("/api/admin/creators/{id}/validate", enAttente).exchange().expectStatus().isForbidden();
    }

    // ── Liste des comptes en attente ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void liste_seulementLesProfilsEnAttente() {
        insertCreator("attente@admin.test", "PENDING");
        insertCreator("valide@admin.test", "VALIDATED");

        // Filtres JsonPath : la base de test peut contenir d'autres profils
        client.get().uri("/api/admin/creators")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.displayName == 'attente@admin.test' && @.validationStatus == 'PENDING')]").exists()
                .jsonPath("$[?(@.displayName == 'valide@admin.test')]").doesNotExist();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void liste_statutInconnu_renvoie400() {
        client.get().uri("/api/admin/creators?status=ARCHIVE").exchange().expectStatus().isBadRequest();
    }

    // ── Scénarios 23 et 24 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void scenario23_valider_rendLeCreateurVisibleDansLaGalerie() {
        long enAttente = insertCreator("attente@admin.test", "PENDING");
        assertThat(visibles()).doesNotContain(enAttente);

        client.post().uri("/api/admin/creators/{id}/validate", enAttente)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.validationStatus").isEqualTo("VALIDATED");

        assertThat(visibles()).contains(enAttente);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void scenario24_refuser_avecMotif_leCreateurResteHorsDeLaGalerie() {
        long enAttente = insertCreator("attente@admin.test", "PENDING");

        client.post().uri("/api/admin/creators/{id}/reject", enAttente)
                .contentType(MediaType.APPLICATION_JSON).body("{\"reason\":\"  Portfolio vide  \"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.validationStatus").isEqualTo("REJECTED")
                .jsonPath("$.rejectionReason").isEqualTo("Portfolio vide");

        assertThat(visibles()).doesNotContain(enAttente);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void refuserSansMotif_renvoie400() {
        long enAttente = insertCreator("attente@admin.test", "PENDING");

        client.post().uri("/api/admin/creators/{id}/reject", enAttente)
                .contentType(MediaType.APPLICATION_JSON).body("{\"reason\":\" \"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void profilDejaTraite_renvoie409_etInconnu_404() {
        long valide = insertCreator("valide@admin.test", "VALIDATED");

        client.post().uri("/api/admin/creators/{id}/validate", valide)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.title").isEqualTo("Profil déjà traité");
        client.post().uri("/api/admin/creators/{id}/validate", 999_999)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    /** Créateurs visibles dans la galerie (même requête que catalog). */
    private Set<Long> visibles() {
        return Set.copyOf(creatorProfileService.findAllValidated(0, 50, Set.of(), CreatorSearchCriteria.AUCUN).items()
                .stream().map(p -> p.userId()).toList());
    }

    private long insertCreator(String email, String status) {
        long id = jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, 'CREATOR') RETURNING id")
                .param("email", email).param("sub", "sub-" + email)
                .query(Long.class).single();
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name, validation_status) VALUES (:id, :name, :status)")
                .param("id", id).param("name", email).param("status", status)
                .update();
        return id;
    }
}
