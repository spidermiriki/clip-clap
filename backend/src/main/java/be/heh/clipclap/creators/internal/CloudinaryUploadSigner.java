package be.heh.clipclap.creators.internal;

import com.cloudinary.Cloudinary;

import java.time.Clock;
import java.util.Map;

/** {@link UploadSigner} adossé à la bibliothèque officielle cloudinary-core : calculs locaux, aucun appel réseau. */
class CloudinaryUploadSigner implements UploadSigner {

    static final String ALLOWED_FORMATS = "jpg,png,webp";

    private final CloudinaryProperties properties;
    private final Clock clock;
    private final Cloudinary cloudinary;

    CloudinaryUploadSigner(CloudinaryProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.cloudinary = properties.isConfigured()
                ? new Cloudinary(Map.of(
                        "cloud_name", properties.cloudName(),
                        "api_key", properties.apiKey(),
                        "api_secret", properties.apiSecret()))
                : null;
    }

    @Override
    public boolean isConfigured() {
        return cloudinary != null;
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
        String signature = cloudinary.apiSignRequest(params, properties.apiSecret(), cloudinary.config.signatureVersion);
        return new UploadSignature(properties.cloudName(), properties.apiKey(), timestamp, signature, folder, ALLOWED_FORMATS);
    }

    @Override
    public boolean isAuthentic(String publicId, String version, String signature) {
        requireConfigured();
        return cloudinary.verifyApiResponseSignature(publicId, version, signature);
    }

    private void requireConfigured() {
        if (cloudinary == null) {
            throw new UploadNotConfiguredException();
        }
    }
}
