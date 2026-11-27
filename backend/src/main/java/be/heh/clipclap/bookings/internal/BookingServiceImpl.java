package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingDetail;
import be.heh.clipclap.offerings.AvailabilityApi;
import be.heh.clipclap.offerings.OfferingApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Création d'une réservation (#29) et transitions de statut, avec journalisation (#39).
 * L'acceptation ou le refus (#31), l'annulation (#35), l'expiration automatique (#36) et le
 * passage à « Terminée » (#37) appelleront {@link #changerStatut} depuis leurs propres points
 * d'entrée (REST ou tâche planifiée) ; aucun n'est encore implémenté.
 */
@Service
class BookingServiceImpl {

    /** Transitions autorisées, statut de départ → statuts d'arrivée possibles. */
    private static final Map<String, Set<String>> TRANSITIONS_AUTORISEES = Map.of(
            "EN_ATTENTE", Set.of("CONFIRMEE", "REFUSEE", "ANNULEE", "EXPIREE"),
            "CONFIRMEE", Set.of("ANNULEE", "TERMINEE")
    );

    private final BookingRepository bookings;
    private final OfferingApi offeringApi;
    private final AvailabilityApi availabilityApi;

    BookingServiceImpl(BookingRepository bookings, OfferingApi offeringApi, AvailabilityApi availabilityApi) {
        this.bookings = bookings;
        this.offeringApi = offeringApi;
        this.availabilityApi = availabilityApi;
    }

    @Transactional
    Booking creer(long clientId, BookingCommand command) {
        if (command == null || command.offeringId() == null || command.slotId() == null) {
            throw new IllegalArgumentException("La prestation et le créneau sont obligatoires");
        }
        var offering = offeringApi.findOffering(command.offeringId())
                .orElseThrow(() -> new OfferingNotFoundException(command.offeringId()));
        if (offering.creatorUserId() == clientId) {
            throw new SelfBookingException();
        }
        availabilityApi.findSlot(command.slotId())
                .orElseThrow(() -> new SlotNotFoundException(command.slotId()));
        if (!availabilityApi.isBookable(command.slotId(), command.offeringId())) {
            throw new SlotNotBookableException(command.slotId());
        }
        Set<Long> optionIds = command.optionIds() == null ? Set.of() : command.optionIds();
        var quote = offeringApi.quote(command.offeringId(), optionIds)
                .orElseThrow(() -> new OfferingNotFoundException(command.offeringId()));

        Booking booking = bookings.create(clientId, offering.creatorUserId(),
                command.offeringId(), command.slotId(), quote.total());
        bookings.insertHistoryEntry(booking.id(), clientId, null, booking.status());
        return booking;
    }

    @Transactional
    Booking changerStatut(long bookingId, long actorId, String nouveauStatut) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        String statutActuel = booking.status();

        if (!TRANSITIONS_AUTORISEES.getOrDefault(statutActuel, Set.of()).contains(nouveauStatut)) {
            throw new InvalidBookingTransitionException(statutActuel, nouveauStatut);
        }

        // La mise à jour porte elle-même la condition de statut (pas de verrou explicite) : si une
        // requête concurrente a déjà changé le statut entre notre lecture et cette écriture, aucune
        // ligne n'est modifiée et aucun historique n'est écrit pour cette transition.
        if (!bookings.updateStatusSiActuel(bookingId, statutActuel, nouveauStatut)) {
            String statutReel = bookings.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException(bookingId))
                    .status();
            throw new InvalidBookingTransitionException(statutReel, nouveauStatut);
        }

        bookings.insertHistoryEntry(bookingId, actorId, statutActuel, nouveauStatut);
        return bookings.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    BookingDetail detail(long bookingId, long requesterId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (requesterId != booking.clientId() && requesterId != booking.creatorId()) {
            throw new BookingAccessDeniedException();
        }

        return new BookingDetail(booking, bookings.findHistory(bookingId));
    }
}
