package be.heh.clipclap.creators.internal;

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

// Les catégories (Mariage, Sport…) sont insérées par data.sql au démarrage
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class CreatorCategoryControllerTest {

    static final String ALICE = "sub-alice@categories.test";

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

    @Test
    @WithMockUser(username = ALICE)
    void choisirPuisRelire_sansDoublon() {
        insertCreator("alice@categories.test", ALICE);
        int mariage = categoryId("Mariage");
        int portrait = categoryId("Portrait");

        client.get().uri("/api/creators/me/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);

        client.put().uri("/api/creators/me/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"categoryIds\":[" + portrait + "," + mariage + "," + mariage + "]}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);

        client.get().uri("/api/creators/me/categories")
                .exchange()
                .expectBody()
                .jsonPath("$[0]").isEqualTo(Math.min(mariage, portrait))
                .jsonPath("$[1]").isEqualTo(Math.max(mariage, portrait));
    }

    @Test
    @WithMockUser(username = ALICE)
    void remplacer_retireLesAnciennes_etLaGalerieFiltreDessus() {
        long alice = insertCreator("alice@categories.test", ALICE);
        int mariage = categoryId("Mariage");
        int sport = categoryId("Sport");
        put("{\"categoryIds\":[" + mariage + "]}");

        put("{\"categoryIds\":[" + sport + "]}");

        assertThat(visibles(sport)).contains(alice);
        assertThat(visibles(mariage)).doesNotContain(alice);

        put("{\"categoryIds\":[]}");
        assertThat(visibles(sport)).doesNotContain(alice);
    }

    @Test
    @WithMockUser(username = ALICE)
    void categorieInconnue_renvoie400ProblemDetail() {
        insertCreator("alice@categories.test", ALICE);

        client.put().uri("/api/creators/me/categories")
                .contentType(MediaType.APPLICATION_JSON).body("{\"categoryIds\":[999999]}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Catégorie inconnue");
    }

    @Test
    @WithMockUser(username = ALICE)
    void listeAbsente_renvoie400() {
        insertCreator("alice@categories.test", ALICE);

        client.put().uri("/api/creators/me/categories")
                .contentType(MediaType.APPLICATION_JSON).body("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = "sub-sans-profil@categories.test")
    void sansProfilCreateur_renvoie404() {
        jdbc.sql("INSERT INTO users (email, sub, role) VALUES ('sans-profil@categories.test', 'sub-sans-profil@categories.test', 'CREATOR')")
                .update();

        client.get().uri("/api/creators/me/categories")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Profil introuvable");
    }

    @Test
    void sansConnexion_renvoie401() {
        client.get().uri("/api/creators/me/categories").exchange().expectStatus().isUnauthorized();
    }

    private void put(String body) {
        client.put().uri("/api/creators/me/categories")
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange()
                .expectStatus().isOk();
    }

    /** Créateurs visibles dans la galerie filtrée par cette catégorie (même requête que catalog). */
    private Set<Long> visibles(int categoryId) {
        var criteria = new CreatorSearchCriteria(null, null, categoryId, null);
        return Set.copyOf(creatorProfileService.findAllValidated(0, 50, Set.of(), criteria).items()
                .stream().map(p -> p.userId()).toList());
    }

    private int categoryId(String name) {
        return jdbc.sql("SELECT id FROM categories WHERE name = :name").param("name", name).query(Integer.class).single();
    }

    private long insertCreator(String email, String sub) {
        long id = jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, 'CREATOR') RETURNING id")
                .param("email", email).param("sub", sub)
                .query(Long.class).single();
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name) VALUES (:id, :name)")
                .param("id", id).param("name", email)
                .update();
        return id;
    }
}
