package be.heh.clipclap.users.internal;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * L'API REST exige un token Zitadel valide (sinon 401). Le reste (route de santé,
 * Swagger) reste public.
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

	// Patterns (pas d'origines exactes) : couvre à la fois le domaine de prod Vercel
	// et ses previews par PR, dont le sous-domaine change à chaque fois (#105)
	@Value("${app.cors.allowed-origin-patterns}")
	private List<String> corsAllowedOriginPatterns;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// API sans session ni cookie : l'identité vient du token à chaque requête
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
				.requestMatchers("/api/**").authenticated()
				.anyRequest().permitAll())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(new ZitadelRoleConverter())));
		return http.build();
	}

	// Frontend et backend sont sur des domaines séparés (Vercel / Render) : sans ceci,
	// le navigateur bloque les appels /api/** avant même qu'ils n'atteignent le serveur
	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(corsAllowedOriginPatterns);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		// Jamais de cookie envoyé (token Bearer uniquement) : pas besoin d'autoriser les credentials
		config.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
