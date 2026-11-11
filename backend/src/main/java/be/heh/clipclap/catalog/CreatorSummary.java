package be.heh.clipclap.catalog;

public record CreatorSummary(
		Long userId,
		String displayName,
		String photoUrl,
		String city,
		String country
) {}
