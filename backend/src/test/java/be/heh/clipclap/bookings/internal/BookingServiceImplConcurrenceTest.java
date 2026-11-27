package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BookingRepository} mocké pour simuler une requête concurrente qui change le statut
 * entre la lecture et l'écriture de {@link BookingServiceImpl#changerStatut} (revue de #126) :
 * impossible à provoquer de façon fiable avec une vraie base dans un test à une seule transaction.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplConcurrenceTest {

    @Mock
    BookingRepository bookings;

    @InjectMocks
    BookingServiceImpl service;

    @Test
    void changerStatut_statutDejaChangeEntreLaLectureEtLEcriture_rejetteSansEcrireLHistorique() {
        long bookingId = 1L;
        Booking luEnAttente = new Booking(bookingId, 10L, 20L, 30L, 40L, null, "EN_ATTENTE", Instant.now());
        Booking reluApresLaCourse = new Booking(bookingId, 10L, 20L, 30L, 40L, null, "ANNULEE", Instant.now());

        when(bookings.findById(bookingId))
                .thenReturn(Optional.of(luEnAttente), Optional.of(reluApresLaCourse));
        // Une autre requête a déjà fait passer la réservation à ANNULEE : la mise à jour conditionnée
        // par le statut lu (EN_ATTENTE) ne modifie donc aucune ligne.
        when(bookings.updateStatusSiActuel(bookingId, "EN_ATTENTE", "CONFIRMEE")).thenReturn(false);

        assertThatThrownBy(() -> service.changerStatut(bookingId, 20L, "CONFIRMEE"))
                .isInstanceOf(InvalidBookingTransitionException.class)
                .hasMessageContaining("ANNULEE");

        verify(bookings, never()).insertHistoryEntry(anyLong(), anyLong(), any(), any());
    }
}
