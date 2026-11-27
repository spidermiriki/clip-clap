package be.heh.clipclap.bookings.internal;

class OfferingNotFoundException extends RuntimeException {
    OfferingNotFoundException(long offeringId) {
        super("Prestation introuvable : " + offeringId);
    }
}
