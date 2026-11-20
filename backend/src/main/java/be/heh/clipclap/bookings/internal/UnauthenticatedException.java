package be.heh.clipclap.bookings.internal;

class UnauthenticatedException extends RuntimeException {
    UnauthenticatedException() {
        super("Authentification requise.");
    }
}
