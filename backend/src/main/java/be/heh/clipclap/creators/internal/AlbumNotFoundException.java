package be.heh.clipclap.creators.internal;

/** Album inexistant ou appartenant à un autre créateur : les deux cas répondent pareil (404). */
class AlbumNotFoundException extends RuntimeException {

    AlbumNotFoundException(long albumId) {
        super("Aucun album " + albumId + " pour ce créateur");
    }
}
