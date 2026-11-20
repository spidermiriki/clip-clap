package be.heh.clipclap.creators.internal;

/**
 * Stockage des fichiers des albums (#94). Derrière une interface pour que les tests n'appellent
 * jamais Cloudinary : ils la remplacent par un mock.
 */
interface MediaStorage {

    /**
     * Supprime le fichier. Un fichier déjà absent n'est pas une erreur.
     *
     * @throws StorageUnavailableException si le stockage ne confirme pas la suppression
     * @throws IllegalArgumentException si le type n'est ni {@code PHOTO} ni {@code VIDEO}
     */
    void delete(String publicId, String mediaType);
}
