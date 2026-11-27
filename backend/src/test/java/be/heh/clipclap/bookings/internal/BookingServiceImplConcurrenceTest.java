package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.offerings.AvailabilityApi;
import be.heh.clipclap.offerings.Offering;
import be.heh.clipclap.offerings.OfferingApi;
import be.heh.clipclap.offerings.PriceQuote;
import be.heh.clipclap.users.UserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BookingRepository} mocké pour simuler des requêtes concurrentes impossibles à
 * provoquer de façon fiable avec une vraie base dans un test à une seule transaction :
 * un changement de statut entre la lecture et l'écriture de
 * {@link BookingServiceImpl#changerStatut} (revue de #126), et une violation de la contrainte
 * d'unicité {@code uq_bookings_slot_active} pendant l'écriture de {@link BookingServiceImpl#creer}
 * (#30, scénario 21).
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplConcurrenceTest {

    @Mock
    BookingRepository bookings;

    @Mock
    OfferingApi offeringApi;

    @Mock
    AvailabilityApi availabilityApi;

    @Mock
    CreatorProfileService creatorProfiles;

    @Mock
    UserApi userApi;

    @Mock
    ApplicationEventPublisher events;

    @InjectMocks
    BookingServiceImpl service;

    @Test
    void changerStatut_statutDejaChangeEntreLaLectureEtLEcriture_rejetteSansEcrireLHistorique() {
        long bookingId = 1L;
        Booking luEnAttente = new Booking(bookingId, 10L, 20L, 30L, 40L, null, "EN_ATTENTE", Instant.now(), null);
        Booking reluApresLaCourse = new Booking(bookingId, 10L, 20L, 30L, 40L, null, "ANNULEE", Instant.now(), null);

        when(bookings.findById(bookingId))
                .thenReturn(Optional.of(luEnAttente), Optional.of(reluApresLaCourse));
        // Une autre requête a déjà fait passer la réservation à ANNULEE : la mise à jour conditionnée
        // par le statut lu (EN_ATTENTE) ne modifie donc aucune ligne.
        when(bookings.updateStatusSiActuel(bookingId, "EN_ATTENTE", "CONFIRMEE", null)).thenReturn(false);

        assertThatThrownBy(() -> service.changerStatut(bookingId, 20L, "CONFIRMEE"))
                .isInstanceOf(InvalidBookingTransitionException.class)
                .hasMessageContaining("ANNULEE");

        verify(bookings, never()).insertHistoryEntry(anyLong(), anyLong(), any(), any());
    }

    @Test
    void creer_contrainteUqBookingsSlotActiveViolee_leveSlotNotBookableException() {
        long clientId = 10L;
        long creatorId = 20L;
        long offeringId = 30L;
        long slotId = 40L;
        var command = new BookingCommand(offeringId, slotId, Set.of());
        var offering = new Offering(offeringId, creatorId, "Prestation", 60, new BigDecimal("100.00"), null, List.of());

        when(offeringApi.findOffering(offeringId)).thenReturn(Optional.of(offering));
        when(availabilityApi.findSlot(slotId)).thenReturn(Optional.of(
                new be.heh.clipclap.offerings.AvailableSlot(slotId, creatorId,
                        java.time.OffsetDateTime.parse("2030-01-01T09:00+00:00"),
                        java.time.OffsetDateTime.parse("2030-01-01T10:00+00:00"))));
        when(availabilityApi.isBookable(slotId, offeringId)).thenReturn(true);
        when(offeringApi.quote(offeringId, Set.of())).thenReturn(
                Optional.of(new PriceQuote(offeringId, offering.basePrice(), List.of(), offering.basePrice())));
        // Une autre réservation vient d'être insérée sur ce créneau entre le recontrôle isBookable
        // et cet appel : la contrainte d'unicité uq_bookings_slot_active (#30) rejette l'INSERT.
        when(bookings.create(clientId, creatorId, offeringId, slotId, offering.basePrice()))
                .thenThrow(new DuplicateKeyException("uq_bookings_slot_active"));

        assertThatThrownBy(() -> service.creer(clientId, command))
                .isInstanceOf(SlotNotBookableException.class);

        verify(bookings, never()).insertHistoryEntry(anyLong(), anyLong(), any(), any());
    }
}
