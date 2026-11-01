package be.heh.clipclap.creators;

public record CreatorProfile(
        Long userId,
        String displayName,
        String photoUrl,
        String bio,
        String city,
        String country,
        String serviceAreas,
        String validationStatus
) {}
