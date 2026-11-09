package be.heh.clipclap.users.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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
 * Le décodeur de tokens et les clients Zitadel sont simulés : aucun appel réel à Zitadel.
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

	@MockitoBean
	ZitadelRoleClient roleClient;

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
	void me_sansRoleDansLeToken_neCreePasDeCompte() {
		givenToken("token-carla", "sub-carla");

		UserAccount account = callMe("token-carla");

		assertThat(account.id()).isNull();
		assertThat(account.role()).isNull();
		assertThat(countUsersWithSub("sub-carla")).isZero();
	}

	@Test
	void me_premiereConnexionAvecRole_creeLeCompte() {
		givenToken("token-carla", "sub-carla", "CLIENT");
		when(userInfoClient.fetchEmail("token-carla")).thenReturn("carla@example.com");

		UserAccount account = callMe("token-carla");

		assertThat(account.email()).isEqualTo("carla@example.com");
		assertThat(account.role()).isEqualTo("CLIENT");
		assertThat(countUsersWithSub("sub-carla")).isEqualTo(1);
	}

	@Test
	void me_connexionSuivante_neCreePasDeDoublon() {
		givenToken("token-david", "sub-david", "CREATOR");
		when(userInfoClient.fetchEmail(anyString())).thenReturn("david@example.com");

		Long firstId = callMe("token-david").id();
		Long secondId = callMe("token-david").id();

		assertThat(secondId).isEqualTo(firstId);
		assertThat(countUsersWithSub("sub-david")).isEqualTo(1);
		verify(userInfoClient, times(1)).fetchEmail(anyString());
	}

	@Test
	void choisirRole_nouvelInscritClient_attribueLeRoleEtCreeLeCompte() {
		givenToken("token-emma", "sub-emma");
		when(userInfoClient.fetchEmail("token-emma")).thenReturn("emma@example.com");

		UserAccount account = callChoisirRole("token-emma", "CLIENT")
			.expectStatus().isOk()
			.expectBody(UserAccount.class)
			.returnResult()
			.getResponseBody();

		assertThat(account).isNotNull();
		assertThat(account.role()).isEqualTo("CLIENT");
		assertThat(countUsersWithSub("sub-emma")).isEqualTo(1);
		verify(roleClient, times(1)).assignRole("sub-emma", "CLIENT");
	}

	@Test
	void choisirRole_nouvelInscritCreateur_attribueLeRoleEtCreeLeCompte() {
		givenToken("token-felix", "sub-felix");
		when(userInfoClient.fetchEmail("token-felix")).thenReturn("felix@example.com");

		UserAccount account = callChoisirRole("token-felix", "CREATOR")
			.expectStatus().isOk()
			.expectBody(UserAccount.class)
			.returnResult()
			.getResponseBody();

		assertThat(account).isNotNull();
		assertThat(account.role()).isEqualTo("CREATOR");
		assertThat(countUsersWithSub("sub-felix")).isEqualTo(1);
		verify(roleClient, times(1)).assignRole("sub-felix", "CREATOR");
	}

	@Test
	void choisirRole_admin_renvoie400() {
		givenToken("token-greg", "sub-greg");

		callChoisirRole("token-greg", "ADMIN")
			.expectStatus().isBadRequest();

		verify(roleClient, never()).assignRole(anyString(), anyString());
	}

	@Test
	void choisirRole_corpsVide_renvoie400() {
		givenToken("token-hugo", "sub-hugo");

		callChoisirRole("token-hugo", null)
			.expectStatus().isBadRequest();

		verify(roleClient, never()).assignRole(anyString(), anyString());
	}

	@Test
	void choisirRole_tokenAvecRoleExistant_renvoie409() {
		givenToken("token-ines", "sub-ines", "CLIENT");

		callChoisirRole("token-ines", "CREATOR")
			.expectStatus().isEqualTo(409);

		verify(roleClient, never()).assignRole(anyString(), anyString());
	}

	@Test
	void routeAdmin_tokenClient_renvoie403() {
		givenToken("token-julie", "sub-julie", "CLIENT");

		client.get().uri("/api/admin/test")
			.header("Authorization", "Bearer token-julie")
			.exchange()
			.expectStatus().isForbidden();
	}

	@Test
	void routeAdmin_tokenCreateur_renvoie403() {
		givenToken("token-karim", "sub-karim", "CREATOR");

		client.get().uri("/api/admin/test")
			.header("Authorization", "Bearer token-karim")
			.exchange()
			.expectStatus().isForbidden();
	}

	@Test
	void routeAdmin_tokenAdmin_neRenvoiePas403() {
		givenToken("token-lea", "sub-lea", "ADMIN");

		client.get().uri("/api/admin/test")
			.header("Authorization", "Bearer token-lea")
			.exchange()
			.expectStatus().value(status -> assertThat(status).isNotEqualTo(403));
	}

	private void givenToken(String tokenValue, String sub, String... roles) {
		Jwt.Builder builder = Jwt.withTokenValue(tokenValue)
			.header("alg", "RS256")
			.subject(sub)
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(300));
		if (roles.length > 0) {
			Map<String, Object> roleClaim = Arrays.stream(roles)
				.collect(Collectors.toMap(role -> role, role -> Map.of()));
			builder.claim("urn:zitadel:iam:org:project:roles", roleClaim);
		}
		when(jwtDecoder.decode(tokenValue)).thenReturn(builder.build());
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

	private RestTestClient.ResponseSpec callChoisirRole(String tokenValue, String role) {
		return client.post().uri("/api/users/me/role")
			.header("Authorization", "Bearer " + tokenValue)
			.body(new AssignRoleRequest(role))
			.exchange();
	}

	private int countUsersWithSub(String sub) {
		return jdbc.sql("SELECT count(*) FROM users WHERE sub = :sub")
			.param("sub", sub)
			.query(Integer.class)
			.single();
	}

}
