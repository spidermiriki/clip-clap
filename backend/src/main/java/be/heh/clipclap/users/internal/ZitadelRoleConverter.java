package be.heh.clipclap.users.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Lit le rôle Zitadel dans le claim {@code urn:zitadel:iam:org:project:roles} (clés = rôles)
 * ou sa variante {@code urn:zitadel:iam:org:project:<projectId>:roles}, et produit les
 * autorités Spring Security correspondantes ({@code ROLE_CLIENT}, {@code ROLE_CREATOR},
 * {@code ROLE_ADMIN}). Un nouvel inscrit n'a encore aucun rôle : le claim est alors absent
 * et aucune autorité n'est produite, sans erreur.
 */
class ZitadelRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final String CLAIM_GENERIQUE = "urn:zitadel:iam:org:project:roles";
	private static final Pattern CLAIM_PAR_PROJET = Pattern.compile("^urn:zitadel:iam:org:project:[^:]+:roles$");
	private static final Set<String> ROLES_CONNUS = Set.of("CLIENT", "CREATOR", "ADMIN");

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = rolesFrom(jwt).stream()
			.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
			.collect(Collectors.toSet());
		return new JwtAuthenticationToken(jwt, authorities);
	}

	/**
	 * Rôles Zitadel connus portés par le token (sans le préfixe {@code ROLE_}). Vide si le
	 * claim de rôles est absent.
	 */
	static Set<String> rolesFrom(Jwt jwt) {
		Set<String> roles = new HashSet<>();
		jwt.getClaims().forEach((nom, valeur) -> {
			if (estUnClaimDeRoles(nom) && valeur instanceof Map<?, ?> rolesDuClaim) {
				rolesDuClaim.keySet().forEach(cle -> roles.add(cle.toString()));
			}
		});
		roles.retainAll(ROLES_CONNUS);
		return roles;
	}

	private static boolean estUnClaimDeRoles(String nomDuClaim) {
		return CLAIM_GENERIQUE.equals(nomDuClaim) || CLAIM_PAR_PROJET.matcher(nomDuClaim).matches();
	}

}
