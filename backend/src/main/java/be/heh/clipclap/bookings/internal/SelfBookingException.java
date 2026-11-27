package be.heh.clipclap.bookings.internal;

/** Un créateur ne peut pas réserver ses propres prestations (scénario 22). */
class SelfBookingException extends RuntimeException {
    SelfBookingException() {
        super("Vous ne pouvez pas réserver votre propre prestation.");
    }
}
