package be.heh.clipclap.creators;

public class CreatorProfileAlreadyExistsException extends RuntimeException {

    public CreatorProfileAlreadyExistsException(Long userId) {
        super("Un profil créateur existe déjà pour l'utilisateur " + userId);
    }
}
