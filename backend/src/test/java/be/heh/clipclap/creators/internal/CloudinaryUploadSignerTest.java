package be.heh.clipclap.creators.internal;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Aucun appel à Cloudinary : la signature est un calcul local. Les attendus sont recalculés ici
// indépendamment de la bibliothèque (SHA-1 des paramètres triés suivis du secret).
class CloudinaryUploadSignerTest {

    static final String SECRET = "secret-de-test";
    static final Clock HORLOGE = Clock.fixed(Instant.ofEpochSecond(1_731_400_000L), ZoneOffset.UTC);

    final CloudinaryUploadSigner signer =
            new CloudinaryUploadSigner(new CloudinaryProperties("cloud-test", "cle-test", SECRET), HORLOGE);

    @Test
    void sign_signeLeDossierLesFormatsEtLHorodatage_sansExposerLeSecret() {
        UploadSignature signature = signer.sign("clicclap/creators/1/albums/2");

        assertThat(signature.timestamp()).isEqualTo(1_731_400_000L);
        assertThat(signature.folder()).isEqualTo("clicclap/creators/1/albums/2");
        assertThat(signature.allowedFormats()).isEqualTo("jpg,png,webp");
        assertThat(signature.cloudName()).isEqualTo("cloud-test");
        assertThat(signature.apiKey()).isEqualTo("cle-test");
        assertThat(signature.signature()).isEqualTo(
                sha1("allowed_formats=jpg,png,webp&folder=clicclap/creators/1/albums/2&timestamp=1731400000" + SECRET));
        assertThat(signature.toString()).doesNotContain(SECRET);
    }

    @Test
    void isAuthentic_accepteLaSignatureDeCloudinary_etRefuseUneSignatureFalsifiee() {
        String valide = sha1("public_id=clicclap/creators/1/albums/2/photo&version=1731400123" + SECRET);

        assertThat(signer.isAuthentic("clicclap/creators/1/albums/2/photo", "1731400123", valide)).isTrue();
        assertThat(signer.isAuthentic("clicclap/creators/1/albums/2/autre", "1731400123", valide)).isFalse();
        assertThat(signer.isAuthentic("clicclap/creators/1/albums/2/photo", "1731400999", valide)).isFalse();
        assertThat(signer.isAuthentic("clicclap/creators/1/albums/2/photo", "1731400123", "0".repeat(40))).isFalse();
    }

    @Test
    void sansIdentifiants_nonConfigure_etSignatureRefusee() {
        var nonConfigure = new CloudinaryUploadSigner(new CloudinaryProperties("", "", ""), HORLOGE);

        assertThat(nonConfigure.isConfigured()).isFalse();
        assertThatThrownBy(() -> nonConfigure.sign("dossier")).isInstanceOf(UploadNotConfiguredException.class);
    }

    static String sha1(String texte) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(texte.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
