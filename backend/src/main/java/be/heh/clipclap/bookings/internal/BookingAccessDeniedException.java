package be.heh.clipclap.bookings.internal;

/** Le client et le créateur associés à la réservation peuvent la consulter, personne d'autre. */
class BookingAccessDeniedException extends RuntimeException {
    BookingAccessDeniedException() {
        super("Cette réservation ne vous appartient pas.");
    }
}
