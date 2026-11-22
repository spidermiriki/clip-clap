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

// catalog dépend directement de creators, users (#75) et offerings (#27) : DIRECT_DEPENDENCIES les charge
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
	void getCreators_filtreParTypeDeMedia() {
		long avecPhoto = insertValidatedCreator("photo@gallery.test", "Avec photo", false);
		insertValidatedCreator("sanspho@gallery.test", "Sans photo", false);
		long album = jdbcClient.sql("INSERT INTO albums (creator_id, name) VALUES (:id, 'Album') RETURNING id")
				.param("id", avecPhoto).query(Long.class).single();
		jdbcClient.sql("INSERT INTO media (album_id, url, cloudinary_public_id) VALUES (:album, 'https://exemple.test/p.jpg', 'p')")
				.param("album", album).update();

		restTestClient.get().uri("/api/catalog/creators?mediaType=photo")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(1)
				.jsonPath("$.items[0].displayName").isEqualTo("Avec photo");
	}

	@Test
	void getCreators_typeDeMediaInconnu_renvoie400ProblemDetail() {
		restTestClient.get().uri("/api/catalog/creators?mediaType=AUDIO")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.detail").isEqualTo("mediaType doit valoir PHOTO ou VIDEO");
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
	void getCreatorProfile_sansToken_renvoieLeProfilPublicSansStatutDeValidation() {
		long userId = insertValidatedCreator("profil@gallery.test", "Profil Public", false);
		jdbcClient.sql("""
				UPDATE creator_profiles
				SET bio = 'Photographe de mariage', city = 'Mons', country = 'Belgique', service_areas = 'Hainaut'
				WHERE user_id = :userId
				""")
				.param("userId", userId)
				.update();

		restTestClient.get().uri("/api/catalog/creators/{userId}", userId)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.userId").isEqualTo(userId)
				.jsonPath("$.displayName").isEqualTo("Profil Public")
				.jsonPath("$.bio").isEqualTo("Photographe de mariage")
				.jsonPath("$.city").isEqualTo("Mons")
				.jsonPath("$.country").isEqualTo("Belgique")
				.jsonPath("$.serviceAreas").isEqualTo("Hainaut")
				.jsonPath("$.validationStatus").doesNotExist()
				.jsonPath("$.rejectionReason").doesNotExist();
	}

	@Test
	void getCreatorProfile_afficheSesAlbumsEtLeursPhotos() {
		long userId = insertValidatedCreator("albums@gallery.test", "Avec albums", false);
		long albumId = jdbcClient.sql("INSERT INTO albums (creator_id, name) VALUES (:userId, 'Mariages') RETURNING id")
				.param("userId", userId).query(Long.class).single();
		for (String photo : new String[] {"photo-1", "photo-2"}) {
			jdbcClient.sql("""
					INSERT INTO media (album_id, url, cloudinary_public_id)
					VALUES (:albumId, :url, :publicId)
					""")
					.param("albumId", albumId)
					.param("url", "https://res.cloudinary.com/cloud-test/image/upload/v1/" + photo + ".jpg")
					.param("publicId", photo)
					.update();
		}

		restTestClient.get().uri("/api/catalog/creators/{userId}", userId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.albums.length()").isEqualTo(1)
				.jsonPath("$.albums[0].name").isEqualTo("Mariages")
				.jsonPath("$.albums[0].photos.length()").isEqualTo(2)
				.jsonPath("$.albums[0].photos[0].url").isEqualTo("https://res.cloudinary.com/cloud-test/image/upload/v1/photo-1.jpg");
	}

	@Test
	void getCreatorProfile_afficheSesPrestationsEtLeursOptions() {
		long userId = insertValidatedCreator("prestations@gallery.test", "Avec prestations", false);
		long offeringId = jdbcClient.sql("""
				INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
				VALUES (:userId, 'Reportage mariage', 480, 1200.00)
				RETURNING id
				""")
				.param("userId", userId).query(Long.class).single();
		jdbcClient.sql("INSERT INTO offering_options (offering_id, name, extra_price) VALUES (:id, 'Drone', 150.00)")
				.param("id", offeringId).update();

		restTestClient.get().uri("/api/catalog/creators/{userId}", userId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.offerings.length()").isEqualTo(1)
				.jsonPath("$.offerings[0].name").isEqualTo("Reportage mariage")
				.jsonPath("$.offerings[0].durationMinutes").isEqualTo(480)
				.jsonPath("$.offerings[0].basePrice").isEqualTo(1200.0)
				.jsonPath("$.offerings[0].options[0].name").isEqualTo("Drone")
				.jsonPath("$.offerings[0].options[0].extraPrice").isEqualTo(150.0);
	}

	@Test
	void getCreatorProfile_inconnu_renvoie404ProblemDetail() {
		restTestClient.get().uri("/api/catalog/creators/{userId}", 999999)
				.exchange()
				.expectStatus().isNotFound()
				.expectBody()
				.jsonPath("$.title").isEqualTo("Créateur introuvable");
	}

	@Test
	void getCreatorProfile_nonValide_renvoie404() {
		long userId = insertValidatedCreator("attente@gallery.test", "En attente", false);
		jdbcClient.sql("UPDATE creator_profiles SET validation_status = 'PENDING' WHERE user_id = :userId")
				.param("userId", userId)
				.update();

		restTestClient.get().uri("/api/catalog/creators/{userId}", userId)
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void getCreatorProfile_suspendu_renvoie404() {
		long userId = insertValidatedCreator("suspendu-profil@gallery.test", "Suspendu", true);

		restTestClient.get().uri("/api/catalog/creators/{userId}", userId)
				.exchange()
				.expectStatus().isNotFound();
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

	private long insertValidatedCreator(String email, String displayName, boolean suspended) {
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
		return userId;
	}
}
