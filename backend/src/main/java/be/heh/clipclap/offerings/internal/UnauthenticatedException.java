package be.heh.clipclap.offerings.internal;

class UnauthenticatedException extends RuntimeException {

    UnauthenticatedException() {
        super("Authentification requise.");
    }
}
