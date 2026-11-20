package be.heh.clipclap.bookings;

import java.time.Instant;

/** Une entrée du journal des statuts (#39). {@code previousStatus} vaut {@code null} à la création. */
public record BookingStatusChange(
        Long actorId,
        String previousStatus,
        String newStatus,
        Instant changedAt
) {}
