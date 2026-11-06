package be.heh.clipclap.users.internal;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Attribue un rôle de projet à un utilisateur dans Zitadel : source de vérité des rôles,
 * attribuée avant la mise à jour de notre base. Utilise l'API de gestion v2
 * ({@code AuthorizationService.CreateAuthorization}), l'endpoint v1 équivalent
 * ({@code POST /management/v1/users/{userId}/grants}) étant marqué déprécié dans la
 * référence Zitadel.
 * <p>
 * Voir <a href="https://zitadel.com/docs/reference/api/authorization/zitadel.authorization.v2.AuthorizationService.CreateAuthorization">
 * la documentation de CreateAuthorization</a>.
 * <p>
 * {@code ZITADEL_SERVICE_TOKEN} et {@code ZITADEL_ORG_ID} n'ont pas de valeur par défaut :
 * sans eux, le démarrage de l'application reste possible (aucune fonctionnalité en dehors de
 * l'attribution de rôle n'en dépend), mais {@link #assignRole} échoue avec un message clair
 * tant que le compte de service Zitadel n'est pas configuré.
 */
@Component
class ZitadelRoleClient {

	private static final String ENDPOINT = "/zitadel.authorization.v2.AuthorizationService/CreateAuthorization";

	private final RestClient restClient;
	private final String serviceToken;
	private final String projectId;
	private final String organizationId;

	ZitadelRoleClient(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
			@Value("${ZITADEL_SERVICE_TOKEN:}") String serviceToken,
			@Value("${ZITADEL_PROJECT_ID:395951731327930510}") String projectId,
			@Value("${ZITADEL_ORG_ID:}") String organizationId) {
		this.restClient = RestClient.create(issuer);
		this.serviceToken = serviceToken;
		this.projectId = projectId;
		this.organizationId = organizationId;
	}

	void assignRole(String userId, String role) {
		if (serviceToken.isBlank() || organizationId.isBlank()) {
			throw new IllegalStateException(
				"ZITADEL_SERVICE_TOKEN et ZITADEL_ORG_ID doivent être définis pour attribuer un rôle dans Zitadel");
		}
		restClient.post()
			.uri(ENDPOINT)
			.header("Authorization", "Bearer " + serviceToken)
			.header("Connect-Protocol-Version", "1")
			.body(new CreateAuthorizationRequest(userId, projectId, organizationId, List.of(role)))
			.retrieve()
			.toBodilessEntity();
	}

	private record CreateAuthorizationRequest(String userId, String projectId, String organizationId, List<String> roleKeys) {
	}

}
