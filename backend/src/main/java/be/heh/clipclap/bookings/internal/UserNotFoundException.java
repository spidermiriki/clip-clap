package be.heh.clipclap.bookings.internal;

class UserNotFoundException extends RuntimeException {
    UserNotFoundException() {
        super("Aucun compte utilisateur associé à ce token.");
    }
}
