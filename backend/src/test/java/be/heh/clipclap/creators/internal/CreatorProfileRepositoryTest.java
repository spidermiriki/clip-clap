package be.heh.clipclap.creators.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static be.heh.clipclap.creators.CreatorSearchCriteria.AUCUN;

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

        var result = repository.findAllValidated(0, 10, Set.of(), AUCUN);

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

        var page0 = repository.findAllValidated(0, 2, Set.of(), AUCUN);
        var page1 = repository.findAllValidated(1, 2, Set.of(), AUCUN);

        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.items()).hasSize(2);
        assertThat(page1.items()).hasSize(1);
    }

    @Test
    void findAllValidated_excludesGivenUserIds_beforePagination() {
        long a = insertTestUser("excl-a@example.com");
        long b = insertTestUser("excl-b@example.com");
        long c = insertTestUser("excl-c@example.com");
        repository.save(a, new CreateProfileCommand("Anna", null, null, null, null));
        repository.save(b, new CreateProfileCommand("Bruno", null, null, null, null));
        repository.save(c, new CreateProfileCommand("Chloé", null, null, null, null));

        var page0 = repository.findAllValidated(0, 2, Set.of(b), AUCUN);
        var page1 = repository.findAllValidated(1, 2, Set.of(b), AUCUN);

        assertThat(page0.totalElements()).isEqualTo(2);
        assertThat(page0.items()).extracting(CreatorProfile::displayName).containsExactly("Anna", "Chloé");
        assertThat(page1.items()).isEmpty();
    }

    @Test
    void findAllValidated_filtreParVilleEtPays_sansTenirCompteDeLaCasse() {
        long mons = insertTestUser("f-mons@example.com");
        long liege = insertTestUser("f-liege@example.com");
        long paris = insertTestUser("f-paris@example.com");
        repository.save(mons, new CreateProfileCommand("Mons", null, "Mons", "Belgique", null));
        repository.save(liege, new CreateProfileCommand("Liège", null, "Liège", "Belgique", null));
        repository.save(paris, new CreateProfileCommand("Paris", null, "Paris", "France", null));

        var parVille = repository.findAllValidated(0, 10, Set.of(), new CreatorSearchCriteria(" mons ", null, null, null));
        var parPays = repository.findAllValidated(0, 10, Set.of(), new CreatorSearchCriteria(null, "BELGIQUE", null, null));

        assertThat(parVille.items()).extracting(CreatorProfile::displayName).containsExactly("Mons");
        assertThat(parVille.totalElements()).isEqualTo(1);
        assertThat(parPays.items()).extracting(CreatorProfile::displayName).containsExactly("Liège", "Mons");
        assertThat(parPays.totalElements()).isEqualTo(2);
    }

    @Test
    void findAllValidated_filtreParTypeDePrestation() {
        long mariage = insertTestUser("f-mariage@example.com");
        long sport = insertTestUser("f-sport@example.com");
        repository.save(mariage, new CreateProfileCommand("Photographe mariage", null, null, null, null));
        repository.save(sport, new CreateProfileCommand("Photographe sport", null, null, null, null));
        int categorieMariage = categoryId("Mariage");
        linkCategory(mariage, categorieMariage);
        linkCategory(sport, categoryId("Sport"));

        var result = repository.findAllValidated(0, 10, Set.of(), new CreatorSearchCriteria(null, null, categorieMariage, null));

        assertThat(result.items()).extracting(CreatorProfile::displayName).containsExactly("Photographe mariage");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void findAllValidated_filtresCombines_etExclusion_seulsLesProfilsQuiRespectentTout() {
        long ok = insertTestUser("c-ok@example.com");
        long autreVille = insertTestUser("c-ville@example.com");
        long autreCategorie = insertTestUser("c-cat@example.com");
        long exclu = insertTestUser("c-exclu@example.com");
        int categorieMariage = categoryId("Mariage");
        for (long id : new long[] {ok, autreVille, autreCategorie, exclu}) {
            String ville = id == autreVille ? "Namur" : "Mons";
            repository.save(id, new CreateProfileCommand("Créateur " + id, null, ville, "Belgique", null));
            linkCategory(id, id == autreCategorie ? categoryId("Sport") : categorieMariage);
        }

        var result = repository.findAllValidated(0, 10, Set.of(exclu),
                new CreatorSearchCriteria("Mons", "Belgique", categorieMariage, null));

        assertThat(result.items()).extracting(CreatorProfile::userId).containsExactly(ok);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void findAllValidated_filtreParTypeDeMedia() {
        long avecPhoto = insertTestUser("m-photo@example.com");
        long albumVide = insertTestUser("m-vide@example.com");
        long sansAlbum = insertTestUser("m-sans@example.com");
        repository.save(avecPhoto, new CreateProfileCommand("Avec photo", null, null, null, null));
        repository.save(albumVide, new CreateProfileCommand("Album vide", null, null, null, null));
        repository.save(sansAlbum, new CreateProfileCommand("Sans album", null, null, null, null));
        long album = insertAlbum(avecPhoto);
        insertAlbum(albumVide);
        jdbcClient.sql("INSERT INTO media (album_id, url, cloudinary_public_id) VALUES (:album, 'https://exemple.test/p.jpg', 'p')")
                  .param("album", album).update();

        var photos = repository.findAllValidated(0, 10, Set.of(), new CreatorSearchCriteria(null, null, null, "PHOTO"));
        var videos = repository.findAllValidated(0, 10, Set.of(), new CreatorSearchCriteria(null, null, null, "VIDEO"));

        assertThat(photos.items()).extracting(CreatorProfile::displayName).containsExactly("Avec photo");
        assertThat(photos.totalElements()).isEqualTo(1);
        assertThat(videos.totalElements()).isZero();
    }

    @Test
    void findAllValidatedUserIds_returnsOnlyValidatedProfiles() {
        long validated = insertTestUser("ids-ok@example.com");
        long pending = insertTestUser("ids-pending@example.com");
        repository.save(validated, new CreateProfileCommand("Valide", null, null, null, null));
        repository.save(pending, new CreateProfileCommand("En attente", null, null, null, null));
        jdbcClient.sql("UPDATE creator_profiles SET validation_status = 'PENDING' WHERE user_id = :id")
                  .param("id", pending).update();

        assertThat(repository.findAllValidatedUserIds()).contains(validated).doesNotContain(pending);
    }


    // Catégories de départ insérées par data.sql
    private int categoryId(String name) {
        return jdbcClient.sql("SELECT id FROM categories WHERE name = :name")
                .param("name", name)
                .query(Integer.class)
                .single();
    }

    private long insertAlbum(long creatorId) {
        return jdbcClient.sql("INSERT INTO albums (creator_id, name) VALUES (:creatorId, 'Album') RETURNING id")
                .param("creatorId", creatorId)
                .query(Long.class)
                .single();
    }

    private void linkCategory(long creatorId, int categoryId) {
        jdbcClient.sql("INSERT INTO creator_categories (creator_id, category_id) VALUES (:creatorId, :categoryId)")
                .param("creatorId", creatorId)
                .param("categoryId", categoryId)
                .update();
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
