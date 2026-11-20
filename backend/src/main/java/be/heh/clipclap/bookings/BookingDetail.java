package be.heh.clipclap.bookings;

import java.util.List;

/** Détail d'une réservation, historique des statuts inclus (du plus ancien au plus récent). */
public record BookingDetail(
        Booking booking,
        List<BookingStatusChange> historique
) {}
