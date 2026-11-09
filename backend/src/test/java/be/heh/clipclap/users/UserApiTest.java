package be.heh.clipclap.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import be.heh.clipclap.TestcontainersConfiguration;

@ApplicationModuleTest
@Transactional
@Import(TestcontainersConfiguration.class)
class UserApiTest {

	@Autowired
	UserApi userApi;

	@Autowired
	JdbcClient jdbc;

	@Test
	void findIdBySub_subConnu_renvoieIdDeLUtilisateur() {
		long id = insertUser("alice@example.com", "zitadel-sub-alice");

		assertThat(userApi.findIdBySub("zitadel-sub-alice")).contains(id);
	}

	@Test
	void findIdBySub_subInconnu_renvoieVide() {
		insertUser("bob@example.com", "zitadel-sub-bob");

		assertThat(userApi.findIdBySub("zitadel-sub-inconnu")).isEmpty();
	}

	private long insertUser(String email, String sub) {
		return jdbc.sql("""
				INSERT INTO users (email, sub, role)
				VALUES (:email, :sub, 'CLIENT')
				RETURNING id
				""")
			.param("email", email)
			.param("sub", sub)
			.query(Long.class)
			.single();
	}

}
