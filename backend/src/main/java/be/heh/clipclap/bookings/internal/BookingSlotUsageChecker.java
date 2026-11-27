package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.offerings.SlotUsageChecker;
import org.springframework.stereotype.Component;

/** Inversion de dépendance (#136) : un créneau réservé (En attente ou Confirmée) ne peut pas être supprimé. */
@Component
class BookingSlotUsageChecker implements SlotUsageChecker {

    private final BookingRepository bookings;

    BookingSlotUsageChecker(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    public boolean hasActiveBookings(long slotId) {
        return bookings.existsActiveForSlot(slotId);
    }
}
