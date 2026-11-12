package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// catalog dépend directement de creators et de users (#75) : DIRECT_DEPENDENCIES charge les deux
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
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
	void getCreators_sansToken_renvoie200Json() {
		insertValidatedCreator("public@gallery.test", "Visible sans connexion");

		restTestClient.get().uri("/api/catalog/creators")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.items[0].displayName").isEqualTo("Visible sans connexion");
	}

	@Test
	void getCreators_createurSuspendu_absentDesItemsEtDuTotal() {
		insertValidatedCreator("actif@gallery.test", "Actif", false);
		insertValidatedCreator("suspendu@gallery.test", "Suspendu", true);

		restTestClient.get().uri("/api/catalog/creators?page=0&size=10")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(1)
				.jsonPath("$.items.length()").isEqualTo(1)
				.jsonPath("$.items[0].displayName").isEqualTo("Actif");
	}

	@Test
	void getCreators_filtresVilleEtPaysCombines() {
		insertValidatedCreator("mons@gallery.test", "À Mons");
		insertValidatedCreator("namur@gallery.test", "À Namur");
		insertValidatedCreator("lille@gallery.test", "À Lille");
		setLocation("mons@gallery.test", "Mons", "Belgique");
		setLocation("namur@gallery.test", "Namur", "Belgique");
		setLocation("lille@gallery.test", "Mons", "France");

		restTestClient.get().uri("/api/catalog/creators?city=mons&country=Belgique")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(1)
				.jsonPath("$.items[0].displayName").isEqualTo("À Mons");
	}

	@Test
	void getCategories_sansToken_renvoieLesTypesDePrestation() {
		restTestClient.get().uri("/api/catalog/categories")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$[?(@.name == 'Mariage')].id").exists();
	}

	@Test
	void getCreators_categoryIdNonNumerique_renvoie400() {
		restTestClient.get().uri("/api/catalog/creators?categoryId=abc")
				.exchange()
				.expectStatus().isBadRequest();
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
		insertValidatedCreator(email, displayName, false);
	}

	private void setLocation(String email, String city, String country) {
		jdbcClient.sql("""
				UPDATE creator_profiles SET city = :city, country = :country
				WHERE user_id = (SELECT id FROM users WHERE email = :email)
				""")
				.param("city", city)
				.param("country", country)
				.param("email", email)
				.update();
	}

	private void insertValidatedCreator(String email, String displayName, boolean suspended) {
		long userId = jdbcClient.sql("""
				INSERT INTO users (email, sub, role, suspended)
				VALUES (:email, :sub, 'CREATOR', :suspended)
				RETURNING id
				""")
				.param("email", email)
				.param("sub", "sub-" + email)
				.param("suspended", suspended)
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
