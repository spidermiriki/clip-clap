package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileAlreadyExistsException;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfilePage;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
class CreatorProfileServiceImpl implements CreatorProfileService {

    private final CreatorProfileRepository repository;

    CreatorProfileServiceImpl(CreatorProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public CreatorProfile create(Long userId, CreateProfileCommand command) {
        if (command.displayName() == null || command.displayName().isBlank()) {
            throw new IllegalArgumentException("displayName ne peut pas être vide");
        }
        if (repository.findByUserId(userId).isPresent()) {
            throw new CreatorProfileAlreadyExistsException(userId);
        }
        return repository.save(userId, command);
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
        return repository.findAllValidated(page, size, excludedUserIds, criteria);
    }
}
