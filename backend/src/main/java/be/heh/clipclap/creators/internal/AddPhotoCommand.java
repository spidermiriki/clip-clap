package be.heh.clipclap.creators.internal;

/**
 * Réponse de Cloudinary relayée par le navigateur (étape 3 de l'upload), vérifiée avant enregistrement.
 * {@code mediaType} : {@code PHOTO} (par défaut) ou {@code VIDEO} (#94).
 */
record AddPhotoCommand(String publicId, String version, String signature, String secureUrl, String mediaType) {}
