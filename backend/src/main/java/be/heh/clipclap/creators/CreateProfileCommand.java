package be.heh.clipclap.creators;

import jakarta.validation.constraints.NotBlank;

public record CreateProfileCommand(
        @NotBlank String displayName,
        String bio,
        String city,
        String country,
        String serviceAreas
) {}
