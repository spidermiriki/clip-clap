package be.heh.clipclap.creators.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.creators.CreateProfileCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class CreatorProfileRepositoryTest {

    @Autowired
    CreatorProfileRepository repository;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void save_thenFindByUserId_roundtrip() {
        long userId = insertTestUser("alice@example.com");
        var command = new CreateProfileCommand("Alice Photo", "Photographe", "Bruxelles", "BE", "Brabant wallon");

        repository.save(userId, command);

        var found = repository.findByUserId(userId);
        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(userId);
        assertThat(found.get().displayName()).isEqualTo("Alice Photo");
        assertThat(found.get().city()).isEqualTo("Bruxelles");
        assertThat(found.get().validationStatus()).isEqualTo("VALIDATED");
    }

    @Test
    void update_modifiesExistingProfile() {
        long userId = insertTestUser("bob@example.com");
        repository.save(userId, new CreateProfileCommand("Bob", "Bio initiale", "Liège", "BE", ""));

        var updated = repository.update(userId, new CreateProfileCommand("Bob Photo", "Nouvelle bio", "Namur", "BE", "Namurois"));

        assertThat(updated.displayName()).isEqualTo("Bob Photo");
        assertThat(updated.city()).isEqualTo("Namur");
        assertThat(updated.bio()).isEqualTo("Nouvelle bio");
    }

    @Test
    void findByUserId_whenAbsent_returnsEmpty() {
        assertThat(repository.findByUserId(99999L)).isEmpty();
    }

    private long insertTestUser(String email) {
        return jdbcClient.sql("""
                INSERT INTO users (email, sub, role)
                VALUES (:email, :sub, 'CREATOR')
                RETURNING id
                """)
                .param("email", email)
                .param("sub", "sub-" + email)
                .query(Long.class)
                .single();
    }
}
