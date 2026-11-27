package be.heh.clipclap.offerings;

import java.time.OffsetDateTime;

/** Période occupée par une réservation active (En attente ou Confirmée, #30), fournie par bookings (#28). */
public record BookedPeriod(OffsetDateTime start, OffsetDateTime end) {}
