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

}
