package be.heh.clipclap.creators.internal;

import be.heh.clipclap.TestcontainersConfiguration;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class CreatorControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbcClient;
    RestTestClient restTestClient;

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient.bindToApplicationContext(context)
                .configureServer(mockMvc -> mockMvc.apply(springSecurity()))
                .build();
    }

    // POST /api/creators/me ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "sub-alice@example.com")
    void create_authenticatedWithValidData_returns201() {
        insertUser("alice@example.com", "sub-alice@example.com");

        restTestClient.post().uri("/api/creators/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"Alice","bio":"","city":"Mons","country":"BE","serviceAreas":""}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.displayName").isEqualTo("Alice");
    }

    @Test
    void create_unauthenticated_returns401() {
        restTestClient.post().uri("/api/creators/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"X","bio":"","city":"","country":"","serviceAreas":""}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(username = "sub-unknown@example.com")
    void create_subNotInDatabase_returns404() {
        restTestClient.post().uri("/api/creators/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"Ghost","bio":"","city":"","country":"","serviceAreas":""}
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @WithMockUser(username = "sub-bob@example.com")
    void create_profileAlreadyExists_returns409() {
        long userId = insertUser("bob@example.com", "sub-bob@example.com");
        insertProfile(userId, "Bob");

        restTestClient.post().uri("/api/creators/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"Bob2","bio":"","city":"","country":"","serviceAreas":""}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @WithMockUser(username = "sub-carol@example.com")
    void create_blankDisplayName_returns400() {
        insertUser("carol@example.com", "sub-carol@example.com");

        restTestClient.post().uri("/api/creators/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"","bio":"","city":"","country":"","serviceAreas":""}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // GET /api/creators/me ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "sub-dave@example.com")
    void getProfile_withExistingProfile_returns200() {
        long userId = insertUser("dave@example.com", "sub-dave@example.com");
        insertProfile(userId, "Dave");

        restTestClient.get().uri("/api/creators/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.displayName").isEqualTo("Dave");
    }

    @Test
    @WithMockUser(username = "sub-eve@example.com")
    void getProfile_withNoProfile_returns404() {
        insertUser("eve@example.com", "sub-eve@example.com");

        restTestClient.get().uri("/api/creators/me")
                .exchange()
                .expectStatus().isNotFound();
    }

    // PUT /api/creators/me ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "sub-frank@example.com")
    void update_withExistingProfile_returns200() {
        long userId = insertUser("frank@example.com", "sub-frank@example.com");
        insertProfile(userId, "Frank");

        restTestClient.put().uri("/api/creators/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"Frank modifié","bio":"","city":"","country":"","serviceAreas":""}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.displayName").isEqualTo("Frank modifié");
    }

    @Test
    @WithMockUser(username = "sub-grace@example.com")
    void update_withNoProfile_returns404() {
        insertUser("grace@example.com", "sub-grace@example.com");

        restTestClient.put().uri("/api/creators/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"Grace","bio":"","city":"","country":"","serviceAreas":""}
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private long insertUser(String email, String sub) {
        return jdbcClient.sql("""
                INSERT INTO users (email, sub, role)
                VALUES (:email, :sub, 'CREATOR')
                RETURNING id
                """)
                .param("email", email)
                .param("sub", sub)
                .query(Long.class)
                .single();
    }

    private void insertProfile(long userId, String displayName) {
        jdbcClient.sql("""
                INSERT INTO creator_profiles (user_id, display_name)
                VALUES (:userId, :displayName)
                """)
                .param("userId", userId)
                .param("displayName", displayName)
                .update();
    }
}
