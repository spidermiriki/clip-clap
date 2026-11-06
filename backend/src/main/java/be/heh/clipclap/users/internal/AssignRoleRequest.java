package be.heh.clipclap.users.internal;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de {@code POST /api/users/me/role}. Le rôle reste une chaîne (pas un enum) afin de
 * distinguer une valeur invalide, rejetée explicitement avec 400 ({@link RoleInvalideException}),
 * d'une erreur de désérialisation générique.
 */
record AssignRoleRequest(@NotBlank String role) {
}
