package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileAlreadyExistsException;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfilePage;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatorProfileServiceImplTest {

    @Mock
    CreatorProfileRepository repository;

    @InjectMocks
    CreatorProfileServiceImpl service;

    private static final Long USER_ID = 42L;
    private static final CreateProfileCommand COMMAND =
            new CreateProfileCommand("Alice Photo", "Bio", "Bruxelles", "BE", "Brabant wallon");
    private static final CreatorProfile PROFILE =
            new CreatorProfile(USER_ID, "Alice Photo", null, "Bio", "Bruxelles", "BE", "Brabant wallon", "VALIDATED");

    // --- create ---

    @Test
    void create_delegatesTo_repositorySave_andReturnsProfile() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(repository.save(USER_ID, COMMAND)).thenReturn(PROFILE);

        var result = service.create(USER_ID, COMMAND);

        assertThat(result).isEqualTo(PROFILE);
        verify(repository).save(USER_ID, COMMAND);
    }

    @Test
    void create_whenProfileAlreadyExists_throwsException() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(PROFILE));

        assertThatThrownBy(() -> service.create(USER_ID, COMMAND))
                .isInstanceOf(CreatorProfileAlreadyExistsException.class);
        verify(repository, never()).save(any(), any());
    }

    @Test
    void create_withBlankDisplayName_throwsIllegalArgumentException() {
        var cmd = new CreateProfileCommand("   ", "bio", "city", "BE", "areas");

        assertThatThrownBy(() -> service.create(USER_ID, cmd))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void create_withNullDisplayName_throwsIllegalArgumentException() {
        var cmd = new CreateProfileCommand(null, "bio", "city", "BE", "areas");

        assertThatThrownBy(() -> service.create(USER_ID, cmd))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    // --- update ---

    @Test
    void update_delegatesTo_repositoryUpdate_andReturnsProfile() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(PROFILE));
        when(repository.update(USER_ID, COMMAND)).thenReturn(PROFILE);

        var result = service.update(USER_ID, COMMAND);

        assertThat(result).isEqualTo(PROFILE);
        verify(repository).update(USER_ID, COMMAND);
    }

    @Test
    void update_whenProfileNotFound_throwsException() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, COMMAND))
                .isInstanceOf(CreatorProfileNotFoundException.class);
        verify(repository, never()).update(any(), any());
    }

    @Test
    void update_withBlankDisplayName_throwsIllegalArgumentException() {
        var cmd = new CreateProfileCommand("", "bio", "city", "BE", "areas");

        assertThatThrownBy(() -> service.update(USER_ID, cmd))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    // --- findAllValidated ---

    @Test
    void findAllValidated_delegatesToRepository_withExcludedIds() {
        var expected = new CreatorProfilePage(List.of(PROFILE), 0, 10, 1L);
        var criteria = new CreatorSearchCriteria("Mons", "Belgique", 1);
        when(repository.findAllValidated(0, 10, Set.of(7L), criteria)).thenReturn(expected);

        assertThat(service.findAllValidated(0, 10, Set.of(7L), criteria)).isEqualTo(expected);
        verify(repository).findAllValidated(0, 10, Set.of(7L), criteria);
    }

    @Test
    void findAllValidatedUserIds_delegatesToRepository() {
        when(repository.findAllValidatedUserIds()).thenReturn(Set.of(USER_ID));

        assertThat(service.findAllValidatedUserIds()).containsExactly(USER_ID);
    }

    @Test
    void findAllValidated_withNegativePage_throwsAndNeverCallsRepository() {
        assertThatThrownBy(() -> service.findAllValidated(-1, 10, Set.of(), CreatorSearchCriteria.AUCUN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void findAllValidated_withZeroSize_throwsAndNeverCallsRepository() {
        assertThatThrownBy(() -> service.findAllValidated(0, 0, Set.of(), CreatorSearchCriteria.AUCUN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void findAllValidated_withSizeAbove50_throwsAndNeverCallsRepository() {
        assertThatThrownBy(() -> service.findAllValidated(0, 51, Set.of(), CreatorSearchCriteria.AUCUN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    // --- findByUserId ---

    @Test
    void findByUserId_whenPresent_returnsProfile() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(PROFILE));

        assertThat(service.findByUserId(USER_ID)).contains(PROFILE);
    }

    @Test
    void findByUserId_whenAbsent_returnsEmpty() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.findByUserId(USER_ID)).isEmpty();
    }
}
