package be.heh.clipclap.creators;

import java.util.Optional;
import java.util.Set;

public interface CreatorProfileService {

    CreatorProfile create(Long userId, CreateProfileCommand command);

    CreatorProfile update(Long userId, CreateProfileCommand command);

    Optional<CreatorProfile> findByUserId(Long userId);

    /** Identifiants utilisateur de tous les profils validés. */
    Set<Long> findAllValidatedUserIds();

    /**
     * Page de profils validés, sans ceux de {@code excludedUserIds} : l'exclusion est faite
     * avant la pagination, donc {@code totalElements} ne compte pas les profils exclus.
     */
    CreatorProfilePage findAllValidated(int page, int size, Set<Long> excludedUserIds);
}
