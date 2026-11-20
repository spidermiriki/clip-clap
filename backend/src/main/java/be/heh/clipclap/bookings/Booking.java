package be.heh.clipclap.bookings;

import java.time.Instant;

public record Booking(
        Long id,
        Long clientId,
        Long creatorId,
        String status,
        Instant createdAt
) {}
