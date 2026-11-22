package be.heh.clipclap.creators.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumServiceImplTest {

    @Mock AlbumRepository albums;
    @Mock CreatorProfileRepository profiles;
    @Mock UploadSigner uploadSigner;
    @InjectMocks AlbumServiceImpl service;

    @Test
    void sansIdentifiantsCloudinary_signatureEtEnregistrementRepondentIndisponible() {
        when(uploadSigner.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.signUpload(1L, 2L, "PHOTO")).isInstanceOf(UploadNotConfiguredException.class);
        assertThatThrownBy(() -> service.addPhoto(1L, 2L, new AddPhotoCommand("p", "1", "s", "u", null)))
                .isInstanceOf(UploadNotConfiguredException.class);
        verifyNoInteractions(albums);
    }

    @Test
    void nomVideOuTropLong_refuse() {
        assertThatThrownBy(() -> service.rename(1L, 2L, "   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.rename(1L, 2L, "x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(albums);
    }
}
