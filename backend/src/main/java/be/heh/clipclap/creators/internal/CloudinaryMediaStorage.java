package be.heh.clipclap.creators.internal;

import com.cloudinary.Configuration;
import com.cloudinary.Util;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.util.Map;
import java.util.Set;

/**
 * {@link MediaStorage} adossé à l'API {@code destroy} de Cloudinary : requête signée avec
 * {@code RestClient}, la signature étant calculée par cloudinary-core (sans son client HTTP,
 * absent du backend, voir {@link CloudinaryUploadSigner}).
 */
class CloudinaryMediaStorage implements MediaStorage {

    // Réponses de Cloudinary qui signifient que le fichier n'existe plus
    private static final Set<String> SUPPRIME = Set.of("ok", "not found");

    private final CloudinaryProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    CloudinaryMediaStorage(CloudinaryProperties properties, RestClient.Builder restClient, Clock clock) {
        this.properties = properties;
        this.restClient = restClient.baseUrl("https://api.cloudinary.com/v1_1").build();
        this.clock = clock;
    }

    @Override
    public void delete(String publicId, String mediaType) {
        // Vérifié avant tout appel : un type inattendu ne doit jamais viser le mauvais endpoint (revue de #125)
        String resourceType = switch (String.valueOf(mediaType)) {
            case "PHOTO" -> "image";
            case "VIDEO" -> "video";
            default -> throw new IllegalArgumentException("Type de média inconnu : " + mediaType);
        };
        if (!properties.isConfigured()) {
            throw new UploadNotConfiguredException();
        }
        long timestamp = clock.instant().getEpochSecond();
        String signature = Util.produceSignature(Map.of("public_id", publicId, "timestamp", timestamp),
                properties.apiSecret(), Configuration.DEFAULT_SIGNATURE_VERSION);

        var form = new LinkedMultiValueMap<String, String>();
        form.add("public_id", publicId);
        form.add("timestamp", String.valueOf(timestamp));
        form.add("api_key", properties.apiKey());
        form.add("signature", signature);

        Map<?, ?> reponse;
        try {
            reponse = restClient.post()
                    .uri("/{cloud}/{type}/destroy", properties.cloudName(), resourceType)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new StorageUnavailableException(e.getClass().getSimpleName());
        }
        Object result = reponse == null ? null : reponse.get("result");
        if (!SUPPRIME.contains(String.valueOf(result))) {
            throw new StorageUnavailableException("réponse « " + result + " »");
        }
    }
}
