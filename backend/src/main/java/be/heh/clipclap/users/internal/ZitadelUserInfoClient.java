package be.heh.clipclap.users.internal;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Lit l'e-mail de l'utilisateur sur l'endpoint userinfo de Zitadel : l'access token
 * ne le contient pas. Appelé une seule fois par utilisateur, à la création du compte.
 */
@Component
class ZitadelUserInfoClient {

	private final RestClient restClient;

	ZitadelUserInfoClient(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
		this.restClient = RestClient.create(issuer);
	}

	String fetchEmail(String accessToken) {
		Map<String, Object> userInfo = restClient.get()
			.uri("/oidc/v1/userinfo")
			.headers(headers -> headers.setBearerAuth(accessToken))
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});
		Object email = userInfo == null ? null : userInfo.get("email");
		if (email == null) {
			throw new IllegalStateException("Zitadel n'a pas renvoyé d'e-mail : vérifier le scope « email »");
		}
		return email.toString();
	}

}
