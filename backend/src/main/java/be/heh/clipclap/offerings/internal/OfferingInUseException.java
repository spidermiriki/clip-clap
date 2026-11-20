package be.heh.clipclap.offerings.internal;

class OfferingInUseException extends RuntimeException {

    OfferingInUseException() {
        super("Cette prestation est liée à une réservation en attente ou confirmée : elle ne peut pas être supprimée.");
    }
}
