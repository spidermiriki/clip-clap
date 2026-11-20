package be.heh.clipclap.offerings;

import java.time.OffsetDateTime;

/** Période occupée par une réservation confirmée, fournie par bookings (#28). */
public record BookedPeriod(OffsetDateTime start, OffsetDateTime end) {}
