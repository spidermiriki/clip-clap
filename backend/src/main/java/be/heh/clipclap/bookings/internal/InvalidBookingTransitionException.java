package be.heh.clipclap.bookings.internal;

class InvalidBookingTransitionException extends RuntimeException {
    InvalidBookingTransitionException(String statutActuel, String statutDemande) {
        super("Transition de statut invalide : " + statutActuel + " → " + statutDemande);
    }
}
