package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
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
		restTestClient = RestTestClient.bindToApplicationContext(context).build();
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
				.expectStatus().isBadRequest();
	}

	private void insertValidatedCreator(String email, String displayName) {
		long userId = jdbcClient.sql("""
				INSERT INTO users (email, password_hash, role)
				VALUES (:email, 'hash', 'CREATOR')
				RETURNING id
				""")
				.param("email", email)
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
