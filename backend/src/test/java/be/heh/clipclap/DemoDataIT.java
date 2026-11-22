package be.heh.clipclap;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

// @SpringBootTest et non @ApplicationModuleTest : les données de démonstration touchent plusieurs
// modules (users, creators, catalog). Le profil « demo » lance son propre contexte, donc sa propre
// base Testcontainers : les autres tests restent sur une base sans ces données.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class DemoDataIT {

	private static final int CREATEURS_VISIBLES = 10;

	@Autowired
	RestTestClient client;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	DataSource dataSource;

	@Test
	void galerie_afficheLesCreateursDeDemo_sansLeSuspenduNiLeProfilEnAttente() {
		client.get().uri("/api/catalog/creators?size=50")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.totalElements").isEqualTo(CREATEURS_VISIBLES)
			.jsonPath("$.items[?(@.displayName == 'Compte Suspendu')]").doesNotExist()
			.jsonPath("$.items[?(@.displayName == 'Profil En Attente')]").doesNotExist();
	}

	@Test
	void filtres_trouventLesCreateursDeDemo() {
		int mariage = jdbc.sql("SELECT id FROM categories WHERE name = 'Mariage'").query(Integer.class).single();

		// Alice (Mons) et Gaëlle (Lille) ; le compte suspendu et le profil en attente restent exclus
		client.get().uri("/api/catalog/creators?categoryId={id}", mariage)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.totalElements").isEqualTo(2);
		client.get().uri("/api/catalog/creators?city=Mons&country=Belgique")
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.totalElements").isEqualTo(2);
	}

	@Test
	void profilPublic_duCompteSuspendu_renvoie404() {
		long suspendu = idDe("compte.suspendu@clicclap.test");

		client.get().uri("/api/catalog/creators/{id}", suspendu)
			.exchange()
			.expectStatus().isNotFound();
	}

	@Test
	void filtreParTypeDeMedia_trouveLesQuatreCreateursAvecPhotos_etCeluiQuiAUneVideo() {
		client.get().uri("/api/catalog/creators?mediaType=PHOTO&size=50")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.totalElements").isEqualTo(4)
			.jsonPath("$.items[?(@.displayName == 'Alice Martin')]").exists()
			.jsonPath("$.items[?(@.displayName == 'Bruno Lambert')]").exists()
			.jsonPath("$.items[?(@.displayName == 'Chloé Dubois')]").exists()
			.jsonPath("$.items[?(@.displayName == 'Inès Moreau')]").exists();
		// Une seule vidéo de démonstration (#94) : le clip de Farid Benali
		client.get().uri("/api/catalog/creators?mediaType=VIDEO")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.totalElements").isEqualTo(1)
			.jsonPath("$.items[0].displayName").isEqualTo("Farid Benali");
	}

	@Test
	void profilPublic_afficheLaVideoDeDemonstration() {
		long farid = idDe("farid.benali@clicclap.test");

		client.get().uri("/api/catalog/creators/{id}", farid)
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.albums.length()").isEqualTo(1)
			.jsonPath("$.albums[0].name").isEqualTo("Clips musicaux")
			.jsonPath("$.albums[0].photos.length()").isEqualTo(1)
			.jsonPath("$.albums[0].photos[0].mediaType").isEqualTo("VIDEO")
			.jsonPath("$.albums[0].photos[?(@.url == 'https://res.cloudinary.com/mcnayzr8/video/upload/samples/cld-sample-video.mp4')]").exists();
	}

	@Test
	void profilPublic_afficheLAlbumDeDepartEtSesPhotosCloudinary() {
		long alice = idDe("alice.martin@clicclap.test");

		client.get().uri("/api/catalog/creators/{id}", alice)
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.albums.length()").isEqualTo(1)
			.jsonPath("$.albums[0].name").isEqualTo("Mariages 2026")
			.jsonPath("$.albums[0].photos.length()").isEqualTo(2)
			.jsonPath("$.albums[0].photos[0].mediaType").isEqualTo("PHOTO")
			.jsonPath("$.albums[0].photos[?(@.url == 'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471943/samples/two-ladies.jpg')]").exists()
			.jsonPath("$.albums[0].photos[?(@.url == 'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471945/samples/smile.jpg')]").exists();
	}

	@Test
	void script_rejoue_sansErreurNiDoublon() {
		long comptesAvant = compter("SELECT COUNT(*) FROM users WHERE email LIKE '%@clicclap.test'");
		long liensAvant = compter("SELECT COUNT(*) FROM creator_categories");
		long albumsAvant = compter("SELECT COUNT(*) FROM albums");
		long mediasAvant = compter("SELECT COUNT(*) FROM media");

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("demo-data.sql"));
		populator.setSqlScriptEncoding("UTF-8");
		populator.execute(dataSource);

		assertThat(comptesAvant).isEqualTo(12);
		assertThat(albumsAvant).isEqualTo(5);
		assertThat(mediasAvant).isEqualTo(9);
		assertThat(compter("SELECT COUNT(*) FROM users WHERE email LIKE '%@clicclap.test'")).isEqualTo(comptesAvant);
		assertThat(compter("SELECT COUNT(*) FROM creator_categories")).isEqualTo(liensAvant);
		assertThat(compter("SELECT COUNT(*) FROM albums")).isEqualTo(albumsAvant);
		assertThat(compter("SELECT COUNT(*) FROM media")).isEqualTo(mediasAvant);
	}

	private long idDe(String email) {
		return jdbc.sql("SELECT id FROM users WHERE email = :email").param("email", email).query(Long.class).single();
	}

	private long compter(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

}
