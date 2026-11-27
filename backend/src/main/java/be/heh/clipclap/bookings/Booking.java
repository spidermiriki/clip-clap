package be.heh.clipclap.bookings;

import java.math.BigDecimal;
import java.time.Instant;

public record Booking(
        Long id,
        Long clientId,
        Long creatorId,
        Long offeringId,
        Long slotId,
        BigDecimal totalPrice,
        String status,
        Instant createdAt
) {}
