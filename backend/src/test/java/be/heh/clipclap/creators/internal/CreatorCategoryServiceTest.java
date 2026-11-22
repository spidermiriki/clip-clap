package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Revue de #122 : seule une clé étrangère violée (23503) devient « catégorie inconnue » ; profil vérifié en premier. */
@ExtendWith(MockitoExtension.class)
class CreatorCategoryServiceTest {

    static final long USER_ID = 7L;

    @Mock CreatorCategoryRepository categories;
    @Mock CreatorProfileRepository profiles;
    @InjectMocks CreatorCategoryService service;

    @BeforeEach
    void setUp() {
        var profil = new CreatorProfile(USER_ID, "Alice", null, null, null, null, null, "VALIDATED", null);
        lenient().when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(profil));
    }

    @Test
    void cleEtrangereViolee_devientCategorieInconnue() {
        doThrow(violation("23503")).when(categories).replace(anyLong(), any());

        assertThatThrownBy(() -> service.replaceMine(USER_ID, List.of(999)))
                .isInstanceOf(UnknownCategoryException.class);
    }

    @Test
    void autreViolation_remonteTelleQuelle() {
        // 23505 : clé primaire en double, par exemple deux PUT simultanés
        doThrow(violation("23505")).when(categories).replace(anyLong(), any());

        assertThatThrownBy(() -> service.replaceMine(USER_ID, List.of(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(UnknownCategoryException.class);
    }

    @Test
    void sansProfil_404AvantLaValidationDeLaListe() {
        assertThatThrownBy(() -> service.replaceMine(99L, null))
                .isInstanceOf(CreatorProfileNotFoundException.class);
        verifyNoInteractions(categories);
    }

    private static DataIntegrityViolationException violation(String sqlState) {
        return new DataIntegrityViolationException("violation", new SQLException("violation", sqlState));
    }
}
