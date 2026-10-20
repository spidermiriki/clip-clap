package be.heh.clipclap.users.internal;

import java.util.Optional;

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
		return jdbc.sql("SELECT id, email, role FROM users WHERE sub = :sub")
			.param("sub", sub)
			.query(UserAccount.class)
			.optional();
	}

	/**
	 * Crée le compte s'il n'existe pas encore. {@code ON CONFLICT} évite un doublon
	 * si deux requêtes de la même personne arrivent en même temps.
	 */
	void insertIfAbsent(String sub, String email, String role) {
		jdbc.sql("""
				INSERT INTO users (sub, email, role)
				VALUES (:sub, :email, :role)
				ON CONFLICT (sub) DO NOTHING
				""")
			.param("sub", sub)
			.param("email", email)
			.param("role", role)
			.update();
	}

}
