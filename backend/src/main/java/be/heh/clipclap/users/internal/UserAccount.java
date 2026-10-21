package be.heh.clipclap.users.internal;

/**
 * Compte local d'un utilisateur, tel que renvoyé par {@code GET /api/users/me}.
 */
record UserAccount(Long id, String email, String role) {
}
