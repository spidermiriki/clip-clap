package be.heh.clipclap.creators.internal;

class UnauthenticatedException extends RuntimeException {
    UnauthenticatedException() {
        super("Authentification requise.");
    }
}
