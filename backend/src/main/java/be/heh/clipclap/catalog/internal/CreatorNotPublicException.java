package be.heh.clipclap.catalog.internal;

/** Créateur inconnu, non validé ou suspendu : pour un visiteur, il n'existe pas. */
class CreatorNotPublicException extends RuntimeException {

	CreatorNotPublicException(long userId) {
		super("Aucun créateur public avec l'identifiant " + userId);
	}
}
