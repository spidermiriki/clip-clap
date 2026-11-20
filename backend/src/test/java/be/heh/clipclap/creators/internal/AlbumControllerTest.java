package be.heh.clipclap.creators.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.creators.Album;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static be.heh.clipclap.creators.internal.CloudinaryUploadSignerTest.sha1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// Identifiants Cloudinary factices : la signature et sa vérification sont locales, et le stockage
// (suppression, #94) est simulé : aucun test n'appelle Cloudinary
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "clicclap.cloudinary.cloud-name=cloud-test",
        "clicclap.cloudinary.api-key=cle-test",
        "clicclap.cloudinary.api-secret=" + AlbumControllerTest.SECRET,
})
@Transactional
class AlbumControllerTest {

    static final String SECRET = "secret-de-test";
    static final String ALICE = "sub-alice@albums.test";
    static final String VERSION = "1731400123";

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @MockitoBean MediaStorage mediaStorage;
    RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToApplicationContext(context)
                .configureServer(mockMvc -> mockMvc.apply(springSecurity()))
                .build();
    }

    // ── Albums ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = ALICE)
    void creerPuisLister_puisRenommer() {
        insertCreator("alice@albums.test", ALICE);

        long albumId = createAlbum("Mariages 2026");
        client.put().uri("/api/creators/me/albums/{id}", albumId)
                .contentType(MediaType.APPLICATION_JSON).body("{\"name\":\"  Mariages  \"}")
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/creators/me/albums")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Mariages")
                .jsonPath("$[0].photos.length()").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = ALICE)
    void creerSansNom_renvoie400() {
        insertCreator("alice@albums.test", ALICE);

        client.post().uri("/api/creators/me/albums")
                .contentType(MediaType.APPLICATION_JSON).body("{\"name\":\" \"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = "sub-sans-profil@albums.test")
    void creerSansProfilCreateur_renvoie404ProfilIntrouvable() {
        insertUser("sans-profil@albums.test", "sub-sans-profil@albums.test");

        client.post().uri("/api/creators/me/albums")
                .contentType(MediaType.APPLICATION_JSON).body("{\"name\":\"Album\"}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Profil introuvable");
    }

    @Test
    void sansConnexion_renvoie401() {
        client.get().uri("/api/creators/me/albums")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── Upload en trois temps ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = ALICE)
    void signature_limiteeAuDossierDeLAlbum_etSansSecret() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Portraits");
        String dossier = "clicclap/creators/" + alice + "/albums/" + albumId;

        UploadSignature signature = client.post().uri("/api/creators/me/albums/{id}/upload-signature", albumId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UploadSignature.class)
                .returnResult().getResponseBody();

        assertThat(signature).isNotNull();
        assertThat(signature.folder()).isEqualTo(dossier);
        assertThat(signature.cloudName()).isEqualTo("cloud-test");
        assertThat(signature.signature()).isEqualTo(
                sha1("allowed_formats=jpg,png,webp&folder=" + dossier + "&timestamp=" + signature.timestamp() + SECRET));

        client.post().uri("/api/creators/me/albums/{id}/upload-signature", albumId)
                .exchange()
                .expectBody().jsonPath("$.apiSecret").doesNotExist();
    }

    @Test
    @WithMockUser(username = ALICE)
    void scenario5_deuxPhotosAuthentiques_visiblesDansLAlbum() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Mariage Dupont");
        String dossier = "clicclap/creators/" + alice + "/albums/" + albumId;

        addPhoto(albumId, photo(dossier + "/photo-1", sha1("public_id=" + dossier + "/photo-1&version=" + VERSION + SECRET)))
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.url").isEqualTo(url(dossier + "/photo-1"));
        addPhoto(albumId, photo(dossier + "/photo-2", sha1("public_id=" + dossier + "/photo-2&version=" + VERSION + SECRET)))
                .expectStatus().isCreated();

        client.get().uri("/api/creators/me/albums")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].photos.length()").isEqualTo(2)
                .jsonPath("$[0].photos[1].url").isEqualTo(url(dossier + "/photo-2"));
    }

    @Test
    @WithMockUser(username = ALICE)
    void signatureFalsifiee_renvoie400_etRienNEstEnregistre() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Album");
        String dossier = "clicclap/creators/" + alice + "/albums/" + albumId;

        addPhoto(albumId, photo(dossier + "/photo", sha1("public_id=" + dossier + "/photo&version=" + VERSION + "mauvais-secret")))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Upload refusé");

        assertThat(compterMedias()).isZero();
    }

    @Test
    @WithMockUser(username = ALICE)
    void mauvaisDossier_renvoie400_memeAvecUneSignatureValide() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Album");
        String autreDossier = "clicclap/creators/" + alice + "/albums/" + (albumId + 1000);

        addPhoto(albumId, photo(autreDossier + "/photo", sha1("public_id=" + autreDossier + "/photo&version=" + VERSION + SECRET)))
                .expectStatus().isBadRequest();

        assertThat(compterMedias()).isZero();
    }

    @Test
    @WithMockUser(username = ALICE)
    void urlHorsDuCompteCloudinary_renvoie400() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Album");
        String publicId = "clicclap/creators/" + alice + "/albums/" + albumId + "/photo";
        String signature = sha1("public_id=" + publicId + "&version=" + VERSION + SECRET);

        addPhoto(albumId, """
                {"publicId":"%s","version":"%s","signature":"%s","secureUrl":"https://exemple.test/v%s/%s.jpg"}
                """.formatted(publicId, VERSION, signature, VERSION, publicId))
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = ALICE)
    void albumDUnAutreCreateur_renvoie404_pourLaSignatureLAjoutEtLeRenommage() {
        insertCreator("alice@albums.test", ALICE);
        long bob = insertCreator("bob@albums.test", "sub-bob@albums.test");
        long albumDeBob = jdbc.sql("INSERT INTO albums (creator_id, name) VALUES (:bob, 'Album de Bob') RETURNING id")
                .param("bob", bob).query(Long.class).single();
        String dossierDeBob = "clicclap/creators/" + bob + "/albums/" + albumDeBob;

        client.post().uri("/api/creators/me/albums/{id}/upload-signature", albumDeBob)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Album introuvable");
        addPhoto(albumDeBob, photo(dossierDeBob + "/photo", sha1("public_id=" + dossierDeBob + "/photo&version=" + VERSION + SECRET)))
                .expectStatus().isNotFound();
        client.put().uri("/api/creators/me/albums/{id}", albumDeBob)
                .contentType(MediaType.APPLICATION_JSON).body("{\"name\":\"Volé\"}")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── Suppression (#94) ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = ALICE)
    void supprimerUnePhoto_laRetireDeCloudinaryPuisDeLAlbum() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Portraits");
        String publicId = "clicclap/creators/" + alice + "/albums/" + albumId + "/photo-1";
        long photoId = insertMedia(albumId, publicId);

        client.delete().uri("/api/creators/me/albums/{albumId}/photos/{mediaId}", albumId, photoId)
                .exchange()
                .expectStatus().isNoContent();

        verify(mediaStorage).delete(publicId, "PHOTO");
        assertThat(compterMedias()).isZero();
    }

    @Test
    @WithMockUser(username = ALICE)
    void supprimerUnAlbum_supprimeSesMediasPuisLAlbum() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Mariage Dupont");
        String dossier = "clicclap/creators/" + alice + "/albums/" + albumId;
        insertMedia(albumId, dossier + "/photo-1");
        insertMedia(albumId, dossier + "/photo-2");

        client.delete().uri("/api/creators/me/albums/{albumId}", albumId)
                .exchange()
                .expectStatus().isNoContent();

        verify(mediaStorage).delete(dossier + "/photo-1", "PHOTO");
        verify(mediaStorage).delete(dossier + "/photo-2", "PHOTO");
        client.get().uri("/api/creators/me/albums")
                .exchange()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = ALICE)
    void stockageIndisponible_renvoie502_etLaPhotoResteDansLAlbum() {
        long alice = insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Portraits");
        long photoId = insertMedia(albumId, "clicclap/creators/" + alice + "/albums/" + albumId + "/photo-1");
        doThrow(new StorageUnavailableException("test")).when(mediaStorage).delete(any(), any());

        client.delete().uri("/api/creators/me/albums/{albumId}/photos/{mediaId}", albumId, photoId)
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody().jsonPath("$.title").isEqualTo("Stockage indisponible");
        client.delete().uri("/api/creators/me/albums/{albumId}", albumId)
                .exchange()
                .expectStatus().isEqualTo(502);

        assertThat(compterMedias()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = ALICE)
    void imageDeDemonstrationHorsDuDossier_retireeDeLaBaseSansToucherCloudinary() {
        insertCreator("alice@albums.test", ALICE);
        long albumId = createAlbum("Démo");
        long photoId = insertMedia(albumId, "samples/smile");

        client.delete().uri("/api/creators/me/albums/{albumId}/photos/{mediaId}", albumId, photoId)
                .exchange()
                .expectStatus().isNoContent();

        verify(mediaStorage, never()).delete(any(), any());
        assertThat(compterMedias()).isZero();
    }

    @Test
    @WithMockUser(username = ALICE)
    void mediaOuAlbumDUnAutre_renvoie404_sansRienSupprimer() {
        insertCreator("alice@albums.test", ALICE);
        long bob = insertCreator("bob@albums.test", "sub-bob@albums.test");
        long albumDeBob = jdbc.sql("INSERT INTO albums (creator_id, name) VALUES (:bob, 'Album de Bob') RETURNING id")
                .param("bob", bob).query(Long.class).single();
        long photoDeBob = insertMedia(albumDeBob, "clicclap/creators/" + bob + "/albums/" + albumDeBob + "/photo");
        long albumDAlice = createAlbum("Portraits");

        client.delete().uri("/api/creators/me/albums/{albumId}", albumDeBob)
                .exchange()
                .expectStatus().isNotFound();
        // Photo de Bob demandée à travers un album d'Alice : le média n'est pas dans cet album
        client.delete().uri("/api/creators/me/albums/{albumId}/photos/{mediaId}", albumDAlice, photoDeBob)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Média introuvable");

        verify(mediaStorage, never()).delete(any(), any());
        assertThat(compterMedias()).isEqualTo(1);
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    private long insertMedia(long albumId, String publicId) {
        return jdbc.sql("""
                INSERT INTO media (album_id, url, cloudinary_public_id)
                VALUES (:albumId, :url, :publicId)
                RETURNING id
                """)
                .param("albumId", albumId)
                .param("url", url(publicId))
                .param("publicId", publicId)
                .query(Long.class)
                .single();
    }

    private long createAlbum(String name) {
        Album album = client.post().uri("/api/creators/me/albums")
                .contentType(MediaType.APPLICATION_JSON).body("{\"name\":\"" + name + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Album.class)
                .returnResult().getResponseBody();
        assertThat(album).isNotNull();
        assertThat(album.name()).isEqualTo(name);
        return album.id();
    }

    private RestTestClient.ResponseSpec addPhoto(long albumId, String body) {
        return client.post().uri("/api/creators/me/albums/{id}/photos", albumId)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange();
    }

    private static String photo(String publicId, String signature) {
        return """
                {"publicId":"%s","version":"%s","signature":"%s","secureUrl":"%s"}
                """.formatted(publicId, VERSION, signature, url(publicId));
    }

    private static String url(String publicId) {
        return "https://res.cloudinary.com/cloud-test/image/upload/v" + VERSION + "/" + publicId + ".jpg";
    }

    private long compterMedias() {
        return jdbc.sql("SELECT COUNT(*) FROM media").query(Long.class).single();
    }

    private long insertUser(String email, String sub) {
        return jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, 'CREATOR') RETURNING id")
                .param("email", email).param("sub", sub)
                .query(Long.class).single();
    }

    private long insertCreator(String email, String sub) {
        long id = insertUser(email, sub);
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name) VALUES (:id, :name)")
                .param("id", id).param("name", email)
                .update();
        return id;
    }
}
