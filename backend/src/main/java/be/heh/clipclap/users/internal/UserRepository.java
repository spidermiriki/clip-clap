package be.heh.clipclap.users.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

	/** Tous les identifiants de comptes suspendus (#98). */
	Set<Long> findAllSuspendedIds() {
		return new HashSet<>(jdbc.sql("SELECT id FROM users WHERE suspended = true")
			.query(Long.class)
			.list());
	}

	/** E-mails de ces utilisateurs (#31), un par identifiant trouvé. */
	Map<Long, String> findEmails(Set<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		List<Map.Entry<Long, String>> lignes = jdbc.sql("SELECT id, email FROM users WHERE id IN (:ids)")
			.param("ids", userIds)
			.query((rs, rowNum) -> Map.entry(rs.getLong("id"), rs.getString("email")))
			.list();
		Map<Long, String> emails = new HashMap<>();
		for (var ligne : lignes) {
			emails.put(ligne.getKey(), ligne.getValue());
		}
		return emails;
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
