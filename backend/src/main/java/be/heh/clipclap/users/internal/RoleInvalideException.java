package be.heh.clipclap.users.internal;

/** Rôle inconnu, ou {@code ADMIN} : un admin ne s'auto-attribue jamais son rôle. */
class RoleInvalideException extends RuntimeException {

	RoleInvalideException(String role) {
		super("Rôle invalide : " + role);
	}

}
