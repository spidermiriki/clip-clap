package be.heh.clipclap.creators.internal;

import com.cloudinary.Configuration;
import com.cloudinary.Util;
import com.cloudinary.api.signing.ApiResponseSignatureVerifier;

import java.time.Clock;
import java.util.Map;

/**
 * {@link UploadSigner} adossé aux classes de signature de cloudinary-core : calculs locaux,
 * aucun appel réseau. On n'instancie pas {@code com.cloudinary.Cloudinary}, qui exige un
 * client HTTP (cloudinary-http5) dont le backend n'a pas besoin pour signer.
 */
class CloudinaryUploadSigner implements UploadSigner {

    static final String ALLOWED_FORMATS = "jpg,png,webp";

    private final CloudinaryProperties properties;
    private final Clock clock;

    CloudinaryUploadSigner(CloudinaryProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public String cloudName() {
        return properties.cloudName();
    }

    @Override
    public UploadSignature sign(String folder) {
        requireConfigured();
        long timestamp = clock.instant().getEpochSecond();
        // Le dossier et les formats sont signés : le navigateur ne peut pas les modifier
        Map<String, Object> params = Map.of(
                "folder", folder,
                "timestamp", timestamp,
                "allowed_formats", ALLOWED_FORMATS);
        String signature = Util.produceSignature(params, properties.apiSecret(), Configuration.DEFAULT_SIGNATURE_VERSION);
        return new UploadSignature(properties.cloudName(), properties.apiKey(), timestamp, signature, folder, ALLOWED_FORMATS);
    }

    @Override
    public boolean isAuthentic(String publicId, String version, String signature) {
        requireConfigured();
        return new ApiResponseSignatureVerifier(properties.apiSecret()).verifySignature(publicId, version, signature);
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new UploadNotConfiguredException();
        }
    }
}
