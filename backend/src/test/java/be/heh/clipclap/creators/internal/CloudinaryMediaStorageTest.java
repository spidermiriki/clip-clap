package be.heh.clipclap.creators.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import static be.heh.clipclap.creators.internal.CloudinaryUploadSignerTest.HORLOGE;
import static be.heh.clipclap.creators.internal.CloudinaryUploadSignerTest.sha1;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Requête de suppression envoyée à Cloudinary (#94), vérifiée sur un serveur simulé : aucun appel réseau. */
class CloudinaryMediaStorageTest {

    static final String SECRET = "secret-de-test";
    static final String PUBLIC_ID = "clicclap/creators/7/albums/3/photo-1";

    MockRestServiceServer serveur;
    CloudinaryMediaStorage storage;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        storage = new CloudinaryMediaStorage(new CloudinaryProperties("cloud-test", "cle-test", SECRET), builder, HORLOGE);
    }

    @Test
    void photo_requeteSigneeVersImageDestroy() {
        long timestamp = HORLOGE.instant().getEpochSecond();
        var attendu = new LinkedMultiValueMap<String, String>();
        attendu.add("public_id", PUBLIC_ID);
        attendu.add("timestamp", String.valueOf(timestamp));
        attendu.add("api_key", "cle-test");
        attendu.add("signature", sha1("public_id=" + PUBLIC_ID + "&timestamp=" + timestamp + SECRET));
        serveur.expect(requestTo("https://api.cloudinary.com/v1_1/cloud-test/image/destroy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(attendu))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        storage.delete(PUBLIC_ID, "PHOTO");

        serveur.verify();
    }

    @Test
    void video_versVideoDestroy_etFichierDejaAbsentAccepte() {
        serveur.expect(requestTo("https://api.cloudinary.com/v1_1/cloud-test/video/destroy"))
                .andRespond(withSuccess("{\"result\":\"not found\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> storage.delete(PUBLIC_ID, "VIDEO")).doesNotThrowAnyException();
        serveur.verify();
    }

    @Test
    void erreurOuReponseInattendue_leveStorageUnavailable() {
        serveur.expect(requestTo("https://api.cloudinary.com/v1_1/cloud-test/image/destroy"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\":{\"message\":\"Invalid Signature\"}}")
                        .contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> storage.delete(PUBLIC_ID, "PHOTO")).isInstanceOf(StorageUnavailableException.class);

        serveur.reset();
        serveur.expect(requestTo("https://api.cloudinary.com/v1_1/cloud-test/image/destroy"))
                .andRespond(withSuccess("{\"result\":\"error\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> storage.delete(PUBLIC_ID, "PHOTO")).isInstanceOf(StorageUnavailableException.class);
    }

    @Test
    void typeInconnu_refuseAvantToutAppel() {
        assertThatThrownBy(() -> storage.delete(PUBLIC_ID, "GIF")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete(PUBLIC_ID, null)).isInstanceOf(IllegalArgumentException.class);
        serveur.verify();
    }

    @Test
    void sansIdentifiants_leveUploadNotConfigured_sansRequete() {
        var nonConfigure = new CloudinaryMediaStorage(new CloudinaryProperties("", "", ""), RestClient.builder(), HORLOGE);

        assertThatThrownBy(() -> nonConfigure.delete(PUBLIC_ID, "PHOTO")).isInstanceOf(UploadNotConfiguredException.class);
    }
}
