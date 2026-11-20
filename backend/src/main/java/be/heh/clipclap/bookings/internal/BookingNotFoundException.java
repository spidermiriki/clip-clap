package be.heh.clipclap.bookings.internal;

class BookingNotFoundException extends RuntimeException {
    BookingNotFoundException(long bookingId) {
        super("Réservation introuvable : " + bookingId);
    }
}
