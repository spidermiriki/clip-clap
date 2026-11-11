package be.heh.clipclap.users.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * L'API REST exige un token Zitadel valide (sinon 401). Le reste (route de santé,
 * Swagger, fichiers du frontend) reste public.
 * <p>
 * Les autorités Spring Security ({@code ROLE_CLIENT}, {@code ROLE_CREATOR}, {@code ROLE_ADMIN})
 * sont dérivées du claim de rôles Zitadel par {@link ZitadelRoleConverter}. Seul
 * {@code /api/admin/**} exige un rôle au niveau de ce filtre : un nouvel inscrit n'a encore
 * aucun rôle, donc {@code /api/users/me} et {@code /api/users/me/role} ne demandent qu'un
 * token valide (l'accès réservé client / créateur, s'il y en a, se fera au niveau de chaque
 * contrôleur, aucun préfixe d'URL de ce type n'étant pour l'instant défini).
 */
@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// API sans session ni cookie : l'identité vient du token à chaque requête
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
				.requestMatchers("/api/**").authenticated()
				.anyRequest().permitAll())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(new ZitadelRoleConverter())));
		return http.build();
	}

}
