package be.heh.clipclap.offerings.internal;

class SlotNotFoundException extends RuntimeException {

    SlotNotFoundException(long slotId) {
        super("Créneau " + slotId + " introuvable");
    }
}
