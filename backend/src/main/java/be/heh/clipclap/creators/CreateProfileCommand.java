package be.heh.clipclap.creators;

public record CreateProfileCommand(
        String displayName,
        String bio,
        String city,
        String country,
        String serviceAreas
) {}
