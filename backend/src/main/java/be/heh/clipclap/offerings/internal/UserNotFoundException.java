package be.heh.clipclap.offerings.internal;

class UserNotFoundException extends RuntimeException {

    UserNotFoundException() {
        super("Aucun compte utilisateur associé à ce token.");
    }
}
