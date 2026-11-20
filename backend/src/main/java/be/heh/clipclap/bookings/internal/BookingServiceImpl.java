package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Création d'une réservation et transitions de statut, avec journalisation (#39). La
 * sélection d'un créneau (#29), l'acceptation ou le refus (#31), l'annulation (#35),
 * l'expiration automatique (#36) et le passage à « Terminée » (#37) appelleront
 * {@link #changerStatut} depuis leurs propres points d'entrée (REST ou tâche planifiée) ;
 * aucun n'est encore implémenté, {@link #creer} et {@link #changerStatut} ne sont donc
 * utilisés que par les tests de ce module pour le moment.
 */
@Service
class BookingServiceImpl {

    /** Transitions autorisées, statut de départ → statuts d'arrivée possibles. */
    private static final Map<String, Set<String>> TRANSITIONS_AUTORISEES = Map.of(
            "EN_ATTENTE", Set.of("CONFIRMEE", "REFUSEE", "ANNULEE", "EXPIREE"),
            "CONFIRMEE", Set.of("ANNULEE", "TERMINEE")
    );

    private final BookingRepository bookings;

    BookingServiceImpl(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Transactional
    Booking creer(long clientId, long creatorId) {
        Booking booking = bookings.create(clientId, creatorId);
        bookings.insertHistoryEntry(booking.id(), clientId, null, booking.status());
        return booking;
    }

    @Transactional
    Booking changerStatut(long bookingId, long actorId, String nouveauStatut) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!TRANSITIONS_AUTORISEES.getOrDefault(booking.status(), Set.of()).contains(nouveauStatut)) {
            throw new InvalidBookingTransitionException(booking.status(), nouveauStatut);
        }

        bookings.updateStatus(bookingId, nouveauStatut);
        bookings.insertHistoryEntry(bookingId, actorId, booking.status(), nouveauStatut);
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
