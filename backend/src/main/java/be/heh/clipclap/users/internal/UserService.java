package be.heh.clipclap.users.internal;

import org.springframework.stereotype.Service;

@Service
class UserService {

	/** Rôle donné à la création ; le choix client / créateur à l'inscription arrive avec #11. */
	static final String DEFAULT_ROLE = "CLIENT";

	private final UserRepository repository;

	private final ZitadelUserInfoClient userInfoClient;

	UserService(UserRepository repository, ZitadelUserInfoClient userInfoClient) {
		this.repository = repository;
		this.userInfoClient = userInfoClient;
	}

	/**
	 * Renvoie le compte local de l'utilisateur connecté et le crée à sa première connexion.
	 */
	UserAccount findOrCreate(String sub, String accessToken) {
		return repository.findBySub(sub).orElseGet(() -> {
			String email = userInfoClient.fetchEmail(accessToken);
			repository.insertIfAbsent(sub, email, DEFAULT_ROLE);
			return repository.findBySub(sub).orElseThrow();
		});
	}

}
