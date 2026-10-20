package be.heh.clipclap.users.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import be.heh.clipclap.TestcontainersConfiguration;

/**
 * Le décodeur de tokens et l'appel userinfo sont simulés : aucun appel réel à Zitadel.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class UserControllerIT {

	@Autowired
	RestTestClient client;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	ZitadelUserInfoClient userInfoClient;

	@Test
	void me_sansToken_renvoie401() {
		client.get().uri("/api/users/me")
			.exchange()
			.expectStatus().isUnauthorized();
	}

	@Test
	void me_tokenInvalide_renvoie401() {
		when(jwtDecoder.decode("token-invalide")).thenThrow(new BadJwtException("signature invalide"));

		client.get().uri("/api/users/me")
			.header("Authorization", "Bearer token-invalide")
			.exchange()
			.expectStatus().isUnauthorized();
	}

	@Test
	void me_premiereConnexion_creeLeCompte() {
		givenToken("token-carla", "sub-carla");
		when(userInfoClient.fetchEmail("token-carla")).thenReturn("carla@example.com");

		UserAccount account = client.get().uri("/api/users/me")
			.header("Authorization", "Bearer token-carla")
			.exchange()
			.expectStatus().isOk()
			.expectBody(UserAccount.class)
			.returnResult()
			.getResponseBody();

		assertThat(account).isNotNull();
		assertThat(account.email()).isEqualTo("carla@example.com");
		assertThat(account.role()).isEqualTo(UserService.DEFAULT_ROLE);
		assertThat(countUsersWithSub("sub-carla")).isEqualTo(1);
	}

	@Test
	void me_connexionSuivante_neCreePasDeDoublon() {
		givenToken("token-david", "sub-david");
		when(userInfoClient.fetchEmail(anyString())).thenReturn("david@example.com");

		Long firstId = callMe("token-david").id();
		Long secondId = callMe("token-david").id();

		assertThat(secondId).isEqualTo(firstId);
		assertThat(countUsersWithSub("sub-david")).isEqualTo(1);
		verify(userInfoClient, times(1)).fetchEmail(anyString());
	}

	private void givenToken(String tokenValue, String sub) {
		Jwt jwt = Jwt.withTokenValue(tokenValue)
			.header("alg", "RS256")
			.subject(sub)
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(300))
			.build();
		when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);
	}

	private UserAccount callMe(String tokenValue) {
		return client.get().uri("/api/users/me")
			.header("Authorization", "Bearer " + tokenValue)
			.exchange()
			.expectStatus().isOk()
			.expectBody(UserAccount.class)
			.returnResult()
			.getResponseBody();
	}

	private int countUsersWithSub(String sub) {
		return jdbc.sql("SELECT count(*) FROM users WHERE sub = :sub")
			.param("sub", sub)
			.query(Integer.class)
			.single();
	}

}
