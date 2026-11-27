package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.offerings.BookedPeriod;
import be.heh.clipclap.offerings.BookedPeriodsProvider;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/** Inversion de dépendance (#28) : offerings retire des disponibilités les réservations confirmées. */
@Component
class BookingPeriodsProvider implements BookedPeriodsProvider {

    private final BookingRepository bookings;

    BookingPeriodsProvider(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    public List<BookedPeriod> findBookedPeriods(long creatorUserId, OffsetDateTime from, OffsetDateTime to) {
        return bookings.findConfirmedPeriods(creatorUserId, from, to);
    }
}
