package be.heh.clipclap.users.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * JUnit pur, sans contexte Spring : le convertisseur ne dépend que de l'objet {@link Jwt}.
 */
class ZitadelRoleConverterTest {

	private final ZitadelRoleConverter converter = new ZitadelRoleConverter();

	@Test
	void convert_claimAbsent_aucuneAutorite() {
		Jwt jwt = jwt(Map.of());

		assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
	}

	@Test
	void convert_claimGenerique_produitLAutoriteDuRole() {
		Jwt jwt = jwt(Map.of("urn:zitadel:iam:org:project:roles",
				Map.of("CREATOR", Map.of("org-id", "org-domain"))));

		assertThat(authorityNames(jwt)).containsExactly("ROLE_CREATOR");
	}

	@Test
	void convert_claimParProjet_produitLAutoriteDuRole() {
		Jwt jwt = jwt(Map.of("urn:zitadel:iam:org:project:123456:roles", Map.of("CLIENT", Map.of())));

		assertThat(authorityNames(jwt)).containsExactly("ROLE_CLIENT");
	}

	@Test
	void convert_cleInconnue_estIgnoree() {
		Jwt jwt = jwt(Map.of("urn:zitadel:iam:org:project:roles", Map.of("SUPER_ADMIN", Map.of())));

		assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
	}

	@Test
	void convert_plusieursRoles_produitToutesLesAutorites() {
		Jwt jwt = jwt(Map.of("urn:zitadel:iam:org:project:roles",
				Map.of("CLIENT", Map.of(), "ADMIN", Map.of())));

		assertThat(authorityNames(jwt)).containsExactlyInAnyOrder("ROLE_CLIENT", "ROLE_ADMIN");
	}

	private Set<String> authorityNames(Jwt jwt) {
		return converter.convert(jwt).getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toSet());
	}

	private Jwt jwt(Map<String, Object> claimsDeRoles) {
		Jwt.Builder builder = Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.subject("sub-test")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(300));
		claimsDeRoles.forEach(builder::claim);
		return builder.build();
	}

}
