package be.heh.clipclap.offerings.internal;

class OfferingNotFoundException extends RuntimeException {

    OfferingNotFoundException(long offeringId) {
        super("Prestation " + offeringId + " introuvable");
    }
}
