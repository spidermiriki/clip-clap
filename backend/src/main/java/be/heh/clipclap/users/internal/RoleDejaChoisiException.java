package be.heh.clipclap.users.internal;

/** Le rôle se choisit une seule fois : le token en porte déjà un. */
class RoleDejaChoisiException extends RuntimeException {

	RoleDejaChoisiException() {
		super("Le rôle a déjà été choisi");
	}

}
