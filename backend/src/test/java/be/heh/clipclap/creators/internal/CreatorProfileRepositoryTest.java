package be.heh.clipclap.creators.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
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

    @Test
    void findAllValidated_excludesNonValidated() {
        long alice = insertTestUser("alice2@example.com");
        long bob   = insertTestUser("bob2@example.com");
        repository.save(alice, new CreateProfileCommand("Alice", null, null, null, null));
        repository.save(bob,   new CreateProfileCommand("Bob",   null, null, null, null));
        jdbcClient.sql("UPDATE creator_profiles SET validation_status = 'PENDING' WHERE user_id = :id")
                  .param("id", bob).update();

        var result = repository.findAllValidated(0, 10);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.items())
                .extracting(CreatorProfile::displayName)
                .containsExactly("Alice");
    }

    @Test
    void findAllValidated_paginates() {
        for (int i = 1; i <= 3; i++) {
            long userId = insertTestUser("pagcreator" + i + "@example.com");
            repository.save(userId, new CreateProfileCommand("Creator " + i, null, null, null, null));
        }

        var page0 = repository.findAllValidated(0, 2);
        var page1 = repository.findAllValidated(1, 2);

        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.items()).hasSize(2);
        assertThat(page1.items()).hasSize(1);
    }

    // À adapter quand users passera à sub (#70)
    private long insertTestUser(String email) {
        return jdbcClient.sql("""
                INSERT INTO users (email, password_hash, role)
                VALUES (:email, 'hash', 'CREATOR')
                RETURNING id
                """)
                .param("email", email)
                .query(Long.class)
                .single();
    }
}
