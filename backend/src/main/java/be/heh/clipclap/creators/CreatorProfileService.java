package be.heh.clipclap.creators;

import java.util.Optional;

public interface CreatorProfileService {

    CreatorProfile create(Long userId, CreateProfileCommand command);

    CreatorProfile update(Long userId, CreateProfileCommand command);

    Optional<CreatorProfile> findByUserId(Long userId);
}
