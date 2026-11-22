package be.heh.clipclap.catalog;

import be.heh.clipclap.creators.Album;
import be.heh.clipclap.offerings.Offering;

import java.util.List;

/**
 * Profil public d'un créateur (wireframe 04), ses albums (#15) et ses prestations (#27). Volontairement sans statut
 * de validation ni motif de refus : ces informations ne regardent que le créateur et l'administrateur.
 */
public record CreatorPublicProfile(
		long userId,
		String displayName,
		String photoUrl,
		String bio,
		String city,
		String country,
		String serviceAreas,
		List<Album> albums,
		List<Offering> offerings
) {}
