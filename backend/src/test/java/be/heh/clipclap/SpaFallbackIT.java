package be.heh.clipclap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

// @SpringBootTest et non @ApplicationModuleTest : test transverse (filtre de sécurité + ressources
// statiques), qui n'appartient à aucun module. Serveur réel (RANDOM_PORT) pour que le forward
// de « / » vers index.html s'exécute vraiment.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class SpaFallbackIT {

	// Présent uniquement dans src/test/resources/static/index.html
	private static final String MARQUEUR_INDEX = "index-spa-test";

	@Autowired
	RestTestClient client;

	@Test
	void racine_renvoieIndexHtml() {
		assertRenvoieIndexHtml("/");
	}

	@Test
	void routesDuFront_auRechargement_renvoientIndexHtml() {
		for (String route : List.of("/mon-profil", "/inscription", "/callback", "/callback?code=abc&state=xyz")) {
			assertRenvoieIndexHtml(route);
		}
	}

	@Test
	void apiInconnue_sansToken_renvoie401SansIndexHtml() {
		client.get().uri("/api/inconnu")
			.exchange()
			.expectStatus().isUnauthorized()
			.expectBody(String.class).value(body -> assertThat(String.valueOf(body)).doesNotContain(MARQUEUR_INDEX));
	}

	// L'exclusion porte sur le premier segment exact, pas sur un préfixe de texte
	@Test
	void routeCommencantParApi_estUneRouteDuFront() {
		assertRenvoieIndexHtml("/apiculture");
	}

	@Test
	void api_seul_neRenvoieJamaisIndexHtml() {
		client.get().uri("/api")
			.exchange()
			.expectBody(String.class).value(body -> assertThat(String.valueOf(body)).doesNotContain(MARQUEUR_INDEX));
	}

	@Test
	void actuatorInconnu_renvoie404SansIndexHtml() {
		client.get().uri("/actuator/inconnu")
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(String.class).value(body -> assertThat(String.valueOf(body)).doesNotContain(MARQUEUR_INDEX));
	}

	@Test
	void fichierStatiqueAbsent_renvoie404() {
		client.get().uri("/assets/absent.js")
			.exchange()
			.expectStatus().isNotFound();
	}

	// /api/inconnu avec un token valide n'est pas testable ici sans vrai jeton Zitadel :
	// on vérifie directement que les chemins serveur sont exclus du fallback
	@Test
	void estRouteDuFront_excluLesCheminsServeurEtLesFichiers() {
		assertThat(List.of("api", "api/inconnu", "actuator/inconnu", "swagger-ui/x", "v3/api-docs", "assets/absent.js"))
			.noneMatch(SpaWebConfig::estRouteDuFront);
		assertThat(List.of("mon-profil", "inscription", "callback", "createurs/12", "apiculture", "actuators", "v3x"))
			.allMatch(SpaWebConfig::estRouteDuFront);
	}

	private void assertRenvoieIndexHtml(String uri) {
		client.get().uri(uri)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
			.expectBody(String.class).value(body -> assertThat(body).contains(MARQUEUR_INDEX));
	}

}
