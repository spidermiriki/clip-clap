package be.heh.clipclap.creators.internal;

/**
 * Paramètres que le navigateur envoie tels quels à Cloudinary (étape 2 de l'upload).
 * Le secret n'en fait jamais partie : seule la signature calculée avec lui est transmise.
 */
record UploadSignature(
        String cloudName,
        String apiKey,
        long timestamp,
        String signature,
        String folder,
        String allowedFormats,
        // « image » ou « video » : l'adresse d'envoi chez Cloudinary (/{resourceType}/upload, #94)
        String resourceType
) {}
