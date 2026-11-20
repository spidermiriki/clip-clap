package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileAlreadyExistsException;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfileNotPendingException;
import be.heh.clipclap.creators.CreatorProfilePage;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
class CreatorProfileServiceImpl implements CreatorProfileService {

    static final Set<String> STATUTS = Set.of("PENDING", "VALIDATED", "REJECTED");
    static final int REASON_MAX_LENGTH = 500;

    private final CreatorProfileRepository repository;

    CreatorProfileServiceImpl(CreatorProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Depuis la v0.2, un nouveau profil attend la validation d'un administrateur (#33). Le statut
     * est fixé ici et non par la valeur par défaut de la colonne, qui reste VALIDATED : la table de
     * production existe déjà, et les données de démonstration fixent leur statut elles-mêmes.
     */
    @Override
    @Transactional
    public CreatorProfile create(Long userId, CreateProfileCommand command) {
        if (command.displayName() == null || command.displayName().isBlank()) {
            throw new IllegalArgumentException("displayName ne peut pas être vide");
        }
        if (repository.findByUserId(userId).isPresent()) {
            throw new CreatorProfileAlreadyExistsException(userId);
        }
        repository.save(userId, command);
        return repository.setStatus(userId, "PENDING");
    }

    @Override
    public CreatorProfile update(Long userId, CreateProfileCommand command) {
        if (command.displayName() == null || command.displayName().isBlank()) {
            throw new IllegalArgumentException("displayName ne peut pas être vide");
        }
        if (repository.findByUserId(userId).isEmpty()) {
            throw new CreatorProfileNotFoundException(userId);
        }
        return repository.update(userId, command);
    }

    @Override
    public Optional<CreatorProfile> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Set<Long> findAllValidatedUserIds() {
        return repository.findAllValidatedUserIds();
    }

    @Override
    public CreatorProfilePage findAllValidated(int page, int size, Set<Long> excludedUserIds,
                                               CreatorSearchCriteria criteria) {
        if (page < 0) throw new IllegalArgumentException("page doit être >= 0");
        if (size < 1 || size > 50) throw new IllegalArgumentException("size doit être compris entre 1 et 50");
        return repository.findAllValidated(page, size, excludedUserIds, withValidMediaType(criteria));
    }

    @Override
    public List<CreatorProfile> findByValidationStatus(String status) {
        String statut = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUTS.contains(statut)) {
            throw new IllegalArgumentException("status doit valoir PENDING, VALIDATED ou REJECTED");
        }
        return repository.findByValidationStatus(statut);
    }

    @Override
    public CreatorProfile validate(Long userId) {
        return decide(userId, "VALIDATED", null);
    }

    @Override
    public CreatorProfile reject(Long userId, String reason) {
        String motif = reason == null ? "" : reason.trim();
        if (motif.isEmpty()) {
            throw new IllegalArgumentException("Le motif du refus est obligatoire");
        }
        if (motif.length() > REASON_MAX_LENGTH) {
            throw new IllegalArgumentException("Le motif du refus dépasse " + REASON_MAX_LENGTH + " caractères");
        }
        return decide(userId, "REJECTED", motif);
    }

    private CreatorProfile decide(Long userId, String status, String reason) {
        return repository.decideIfPending(userId, status, reason).orElseThrow(() ->
                repository.findByUserId(userId).isPresent()
                        ? new CreatorProfileNotPendingException(userId)
                        : new CreatorProfileNotFoundException(userId));
    }

    private static CreatorSearchCriteria withValidMediaType(CreatorSearchCriteria criteria) {
        if (!StringUtils.hasText(criteria.mediaType())) {
            return criteria;
        }
        String type = criteria.mediaType().trim().toUpperCase(Locale.ROOT);
        if (!type.equals("PHOTO") && !type.equals("VIDEO")) {
            throw new IllegalArgumentException("mediaType doit valoir PHOTO ou VIDEO");
        }
        return new CreatorSearchCriteria(criteria.city(), criteria.country(), criteria.categoryId(), type);
    }
}
