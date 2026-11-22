package be.heh.clipclap.creators;

/** Validation ou refus d'un profil qui n'est pas (ou plus) en attente (#33). */
public class CreatorProfileNotPendingException extends RuntimeException {

    public CreatorProfileNotPendingException(Long userId) {
        super("Le profil créateur " + userId + " n'est pas en attente de validation");
    }
}
