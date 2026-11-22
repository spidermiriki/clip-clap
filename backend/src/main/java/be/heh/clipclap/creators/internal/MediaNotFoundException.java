package be.heh.clipclap.creators.internal;

class MediaNotFoundException extends RuntimeException {

    MediaNotFoundException(long mediaId) {
        super("Média " + mediaId + " introuvable dans cet album");
    }
}
