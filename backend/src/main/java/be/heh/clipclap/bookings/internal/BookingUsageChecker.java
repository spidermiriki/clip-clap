package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.offerings.OfferingUsageChecker;
import org.springframework.stereotype.Component;

/** Inversion de dépendance (#27) : une prestation réservée (En attente ou Confirmée) ne peut pas être supprimée. */
@Component
class BookingUsageChecker implements OfferingUsageChecker {

    private final BookingRepository bookings;

    BookingUsageChecker(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    public boolean hasActiveBookings(long offeringId) {
        return bookings.existsActiveForOffering(offeringId);
    }
}
