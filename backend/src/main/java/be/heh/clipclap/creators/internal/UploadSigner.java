package be.heh.clipclap.creators.internal;

/** Signature des uploads directs vers le stockage des photos et des vidéos, et vérification de ses réponses. */
interface UploadSigner {

    boolean isConfigured();

    String cloudName();

    /** Signe un upload limité à {@code folder} et aux formats autorisés pour ce type ({@code PHOTO} ou {@code VIDEO}). */
    UploadSignature sign(String folder, String mediaType);

    /** Vrai si {@code signature} a bien été produite par le stockage pour ce {@code publicId} et cette {@code version}. */
    boolean isAuthentic(String publicId, String version, String signature);
}
