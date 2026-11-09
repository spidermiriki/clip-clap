package be.heh.clipclap.creators.internal;

class UserNotFoundException extends RuntimeException {
    UserNotFoundException() {
        super("Aucun compte utilisateur associé à ce token.");
    }
}
