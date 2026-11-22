package be.heh.clipclap.creators.internal;

class StorageUnavailableException extends RuntimeException {

    StorageUnavailableException(String detail) {
        super("Le stockage des médias n'a pas confirmé la suppression : " + detail + ". Réessayez plus tard.");
    }

    private StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Suppression d'album interrompue : dit ce qui a déjà été supprimé et qu'il faut relancer (revue de #125). */
    static StorageUnavailableException partielle(int supprimes, int total, StorageUnavailableException cause) {
        return new StorageUnavailableException("Suppression de l'album interrompue : " + supprimes + " média(s) sur "
                + total + " supprimé(s), l'album et les autres médias sont conservés. Le stockage des médias n'a pas"
                + " répondu : relancez la suppression de l'album pour la terminer.", cause);
    }
}
