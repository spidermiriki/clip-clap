package be.heh.clipclap.users.internal;

/**
 * Compte local d'un utilisateur, tel que renvoyé par {@code GET /api/users/me}. Avant que
 * le rôle ne soit choisi (inscription en cours), {@code id}, {@code email} et {@code role}
 * valent {@code null} : le compte n'est pas encore inséré en base, la colonne {@code role}
 * étant {@code NOT NULL}.
 */
record UserAccount(Long id, String email, String role, boolean suspended) {
}
