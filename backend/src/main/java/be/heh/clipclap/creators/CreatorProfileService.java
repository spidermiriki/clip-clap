package be.heh.clipclap.creators;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CreatorProfileService {

    CreatorProfile create(Long userId, CreateProfileCommand command);

    CreatorProfile update(Long userId, CreateProfileCommand command);

    Optional<CreatorProfile> findByUserId(Long userId);

    /**
     * Page de profils validés correspondant à {@code criteria}, sans ceux de
     * {@code excludedUserIds} : exclusion et filtres sont appliqués avant la pagination,
     * donc {@code totalElements} ne compte que les profils retenus.
     */
    CreatorProfilePage findAllValidated(int page, int size, Set<Long> excludedUserIds, CreatorSearchCriteria criteria);

    /**
     * Profils dans ce statut de validation ({@code PENDING}, {@code VALIDATED} ou {@code REJECTED}),
     * du plus ancien au plus récent (#33).
     */
    List<CreatorProfile> findByValidationStatus(String status);

    /**
     * Valide un profil en attente : il devient visible dans la galerie (#33).
     *
     * @throws CreatorProfileNotFoundException si le profil n'existe pas
     * @throws CreatorProfileNotPendingException s'il n'est pas en attente
     */
    CreatorProfile validate(Long userId);

    /**
     * Refuse un profil en attente avec un motif, que le créateur voit dans « Mon profil » (#33).
     *
     * @throws CreatorProfileNotFoundException si le profil n'existe pas
     * @throws CreatorProfileNotPendingException s'il n'est pas en attente
     */
    CreatorProfile reject(Long userId, String reason);
}
