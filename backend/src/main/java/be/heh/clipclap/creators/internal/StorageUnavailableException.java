package be.heh.clipclap.creators.internal;

class StorageUnavailableException extends RuntimeException {

    StorageUnavailableException(String detail) {
        super("Le stockage des médias n'a pas confirmé la suppression : " + detail + ". Réessayez plus tard.");
    }
}
