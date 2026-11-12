package be.heh.clipclap.catalog;

/**
 * Profil public d'un créateur (wireframe 04). Volontairement sans statut de validation
 * ni motif de refus : ces informations ne regardent que le créateur et l'administrateur.
 */
public record CreatorPublicProfile(
		long userId,
		String displayName,
		String photoUrl,
		String bio,
		String city,
		String country,
		String serviceAreas
) {}
