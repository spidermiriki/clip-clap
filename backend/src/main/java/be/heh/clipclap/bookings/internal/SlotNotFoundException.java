package be.heh.clipclap.bookings.internal;

class SlotNotFoundException extends RuntimeException {
    SlotNotFoundException(long slotId) {
        super("Créneau introuvable : " + slotId);
    }
}
