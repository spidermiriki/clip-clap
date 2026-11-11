package be.heh.clipclap.users;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

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

	@Test
	void findSuspendedIds_quelquesSuspendus_renvoieSeulementCeuxLa() {
		long suspendu = insertUser("suspendu@example.com", "zitadel-sub-suspendu", true);
		long actif = insertUser("actif@example.com", "zitadel-sub-actif", false);

		assertThat(userApi.findSuspendedIds(Set.of(suspendu, actif))).containsExactly(suspendu);
	}

	@Test
	void findSuspendedIds_aucunSuspenduParmiLesIdsDemandes_renvoieVide() {
		long actif = insertUser("actif2@example.com", "zitadel-sub-actif2", false);

		assertThat(userApi.findSuspendedIds(Set.of(actif))).isEmpty();
	}

	@Test
	void findSuspendedIds_ensembleVide_renvoieVide() {
		assertThat(userApi.findSuspendedIds(Set.of())).isEmpty();
	}

	private long insertUser(String email, String sub) {
		return insertUser(email, sub, false);
	}

	private long insertUser(String email, String sub, boolean suspended) {
		return jdbc.sql("""
				INSERT INTO users (email, sub, role, suspended)
				VALUES (:email, :sub, 'CLIENT', :suspended)
				RETURNING id
				""")
			.param("email", email)
			.param("sub", sub)
			.param("suspended", suspended)
			.query(Long.class)
			.single();
	}

}
