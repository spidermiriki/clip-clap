package be.heh.clipclap.bookings;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Vue enrichie d'une réservation (#31), partagée par le tableau de bord créateur et, plus tard,
 * le suivi client (#34). {@code offering}/{@code slot} valent {@code null} si la prestation ou
 * le créneau a été supprimé depuis (ON DELETE SET NULL, #133).
 */
public record BookingView(
        long id,
        String status,
        BigDecimal totalPrice,
        Instant createdAt,
        OfferingSummary offering,
        SlotSummary slot,
        ClientSummary client,
        CreatorSummary creator,
        String refusalMessage
) {
    public record OfferingSummary(long id, String name) {}

    public record SlotSummary(OffsetDateTime start, OffsetDateTime end) {}

    public record ClientSummary(long id, String email) {}

    public record CreatorSummary(long userId, String displayName) {}
}
