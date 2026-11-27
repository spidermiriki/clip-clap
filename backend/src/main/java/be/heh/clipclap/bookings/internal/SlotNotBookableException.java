package be.heh.clipclap.bookings.internal;

/** Le créneau existe mais n'est plus réservable pour cette prestation (préavis, marge, créateur...). */
class SlotNotBookableException extends RuntimeException {
    SlotNotBookableException(long slotId) {
        super("Ce créneau n'est plus réservable : " + slotId);
    }
}
