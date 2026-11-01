package be.heh.clipclap.creators;

public class CreatorProfileNotFoundException extends RuntimeException {

    public CreatorProfileNotFoundException(Long userId) {
        super("Aucun profil créateur trouvé pour l'utilisateur " + userId);
    }
}
