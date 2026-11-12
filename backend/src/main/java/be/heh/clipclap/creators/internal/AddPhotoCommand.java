package be.heh.clipclap.creators.internal;

/** Réponse de Cloudinary relayée par le navigateur (étape 3 de l'upload), vérifiée avant enregistrement. */
record AddPhotoCommand(String publicId, String version, String signature, String secureUrl) {}
