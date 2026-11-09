package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// extraIncludes = "users" : catalog → creators → users (transitive, non chargé par DIRECT_DEPENDENCIES)
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "users")
@Import(TestcontainersConfiguration.class)
@Transactional
class GalleryControllerTest {

	@Autowired
	WebApplicationContext context;

	@Autowired
	JdbcClient jdbcClient;

	RestTestClient restTestClient;

	@BeforeEach
	void setUp() {
		restTestClient = RestTestClient.bindToApplicationContext(context)
				.configureServer(mockMvc -> mockMvc.apply(springSecurity()))
				.build();
	}

	@Test
	void getCreators_returnsEmptyPageWhenNoCreators() {
		restTestClient.get().uri("/api/catalog/creators")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(0)
				.jsonPath("$.items").isArray()
				.jsonPath("$.items.length()").isEqualTo(0);
	}

	@Test
	void getCreators_returnsValidatedCreatorsWhenPresent() {
		insertValidatedCreator("alice@gallery.test", "Alice Photo");

		restTestClient.get().uri("/api/catalog/creators?page=0&size=10")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(1)
				.jsonPath("$.items[0].displayName").isEqualTo("Alice Photo");
	}

	@Test
	void getCreators_withOversizedSize_returns400() {
		restTestClient.get().uri("/api/catalog/creators?size=100")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.detail").isEqualTo("size doit être compris entre 1 et 50");
	}

	private void insertValidatedCreator(String email, String displayName) {
		long userId = jdbcClient.sql("""
				INSERT INTO users (email, sub, role)
				VALUES (:email, :sub, 'CREATOR')
				RETURNING id
				""")
				.param("email", email)
				.param("sub", "sub-" + email)
				.query(Long.class)
				.single();
		jdbcClient.sql("""
				INSERT INTO creator_profiles (user_id, display_name)
				VALUES (:userId, :displayName)
				""")
				.param("userId", userId)
				.param("displayName", displayName)
				.update();
	}
}
