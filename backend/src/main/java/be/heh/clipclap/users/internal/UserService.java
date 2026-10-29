package be.heh.clipclap.users.internal;

import java.util.Optional;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
class UserService {

	/** Rôles qu'un inscrit peut choisir lui-même ; ADMIN s'attribue autrement. */
	private static final Set<String> ROLES_ATTRIBUABLES = Set.of("CLIENT", "CREATOR");

	private final UserRepository repository;

	private final ZitadelUserInfoClient userInfoClient;

	private final ZitadelRoleClient roleClient;

	UserService(UserRepository repository, ZitadelUserInfoClient userInfoClient, ZitadelRoleClient roleClient) {
		this.repository = repository;
		this.userInfoClient = userInfoClient;
		this.roleClient = roleClient;
	}

	/**
	 * Renvoie le compte local de l'utilisateur connecté. Le rôle vient du token, pas d'une
	 * valeur par défaut : un nouvel inscrit sans rôle encore choisi n'est pas inséré en base
	 * (la colonne {@code role} est {@code NOT NULL}) et reçoit un compte avec {@code role: null}.
	 * Si le rôle du token diffère de celui enregistré, le token fait foi (le rôle se choisit
	 * dans Zitadel) et la base est synchronisée.
	 */
	UserAccount findOrCreate(Jwt jwt) {
		String sub = jwt.getSubject();
		String roleDuToken = rolePrincipal(jwt);
		Optional<UserAccount> compte = repository.findBySub(sub);

		if (compte.isPresent()) {
			UserAccount existant = compte.get();
			if (roleDuToken != null && !roleDuToken.equals(existant.role())) {
				repository.updateRole(sub, roleDuToken);
				return repository.findBySub(sub).orElseThrow();
			}
			return existant;
		}

		if (roleDuToken == null) {
			return new UserAccount(null, null, null);
		}

		String email = userInfoClient.fetchEmail(jwt.getTokenValue());
		repository.upsertRole(sub, email, roleDuToken);
		return repository.findBySub(sub).orElseThrow();
	}

	/**
	 * Attribue le rôle choisi à l'inscription : dans Zitadel d'abord (source de vérité des
	 * rôles), puis dans notre base. Un admin ne s'auto-attribue jamais son rôle (400), et le
	 * rôle ne se choisit qu'une seule fois (409 si le token en porte déjà un).
	 */
	UserAccount choisirRole(Jwt jwt, String roleChoisi) {
		if (!ZitadelRoleConverter.rolesFrom(jwt).isEmpty()) {
			throw new RoleDejaChoisiException();
		}
		if (roleChoisi == null || !ROLES_ATTRIBUABLES.contains(roleChoisi)) {
			throw new RoleInvalideException(roleChoisi);
		}

		String sub = jwt.getSubject();
		String email = repository.findBySub(sub)
			.map(UserAccount::email)
			.orElseGet(() -> userInfoClient.fetchEmail(jwt.getTokenValue()));

		roleClient.assignRole(sub, roleChoisi);
		repository.upsertRole(sub, email, roleChoisi);
		return repository.findBySub(sub).orElseThrow();
	}

	/** Rôle à retenir si le token en porte plusieurs : ADMIN prime sur CREATOR puis CLIENT. */
	private static String rolePrincipal(Jwt jwt) {
		Set<String> roles = ZitadelRoleConverter.rolesFrom(jwt);
		if (roles.contains("ADMIN")) {
			return "ADMIN";
		}
		if (roles.contains("CREATOR")) {
			return "CREATOR";
		}
		if (roles.contains("CLIENT")) {
			return "CLIENT";
		}
		return null;
	}

}
