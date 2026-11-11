package be.heh.clipclap.users.internal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class UserRepository {

	private final JdbcClient jdbc;

	UserRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	Optional<Long> findIdBySub(String sub) {
		return jdbc.sql("SELECT id FROM users WHERE sub = :sub")
			.param("sub", sub)
			.query(Long.class)
			.optional();
	}

	Optional<UserAccount> findBySub(String sub) {
		return jdbc.sql("SELECT id, email, role, suspended FROM users WHERE sub = :sub")
			.param("sub", sub)
			.query(UserAccount.class)
			.optional();
	}

	/** Parmi ces identifiants, ceux dont le compte est suspendu (#75). */
	Set<Long> findSuspendedIds(Set<Long> userIds) {
		if (userIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(jdbc.sql("SELECT id FROM users WHERE id IN (:ids) AND suspended = true")
			.param("ids", userIds)
			.query(Long.class)
			.list());
	}

	/**
	 * Crée le compte avec ce rôle s'il n'existe pas encore, ou met à jour son rôle sinon.
	 * {@code ON CONFLICT} évite un doublon si deux requêtes de la même personne arrivent
	 * en même temps.
	 */
	void upsertRole(String sub, String email, String role) {
		jdbc.sql("""
				INSERT INTO users (sub, email, role)
				VALUES (:sub, :email, :role)
				ON CONFLICT (sub) DO UPDATE SET role = EXCLUDED.role
				""")
			.param("sub", sub)
			.param("email", email)
			.param("role", role)
			.update();
	}

	/** Synchronise le rôle local sur celui du token, qui fait foi. */
	void updateRole(String sub, String role) {
		jdbc.sql("UPDATE users SET role = :role WHERE sub = :sub")
			.param("role", role)
			.param("sub", sub)
			.update();
	}

}
