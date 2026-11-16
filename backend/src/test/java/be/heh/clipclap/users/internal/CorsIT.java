package be.heh.clipclap.users.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import be.heh.clipclap.TestcontainersConfiguration;

/**
 * Frontend (Vercel) et backend (Render) sur deux origines distinctes (#108) : sans CORS, le
 * navigateur bloque les appels. Le `CorsFilter` de Spring Security répond aux pré-requêtes
 * OPTIONS avant tout contrôle d'authentification, y compris sur les routes protégées.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "app.cors.allowed-origin-patterns=https://front.example")
class CorsIT {

	private static final String ORIGINE_AUTORISEE = "https://front.example";
	private static final String ORIGINE_NON_AUTORISEE = "https://pas-autorise.example";

	@Autowired
	RestTestClient client;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void preRequeteOptions_origineAutorisee_renvoie200AvecAutorisationCors() {
		client.options().uri("/api/users/me")
			.header("Origin", ORIGINE_AUTORISEE)
			.header("Access-Control-Request-Method", "GET")
			.header("Access-Control-Request-Headers", "authorization")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("Access-Control-Allow-Origin", ORIGINE_AUTORISEE)
			.expectHeader().valueMatches("Access-Control-Allow-Headers", "(?i).*authorization.*");
	}

	@Test
	void preRequeteOptions_origineNonAutorisee_renvoie403SansAutorisationCors() {
		client.options().uri("/api/users/me")
			.header("Origin", ORIGINE_NON_AUTORISEE)
			.header("Access-Control-Request-Method", "GET")
			.exchange()
			.expectStatus().isForbidden()
			.expectHeader().doesNotExist("Access-Control-Allow-Origin");
	}

	@Test
	void getCatalogCategories_origineAutorisee_renvoie200AvecAutorisationCors() {
		client.get().uri("/api/catalog/categories")
			.header("Origin", ORIGINE_AUTORISEE)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().valueEquals("Access-Control-Allow-Origin", ORIGINE_AUTORISEE);
	}

	@Test
	void getUsersMe_sansTokenOrigineAutorisee_renvoie401AvecAutorisationCors() {
		// Sans l'en-tête CORS sur le 401 lui-même, le navigateur ne peut pas lire la réponse
		client.get().uri("/api/users/me")
			.header("Origin", ORIGINE_AUTORISEE)
			.exchange()
			.expectStatus().isUnauthorized()
			.expectHeader().valueEquals("Access-Control-Allow-Origin", ORIGINE_AUTORISEE);
	}

}
