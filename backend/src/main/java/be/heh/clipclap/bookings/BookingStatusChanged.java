package be.heh.clipclap.bookings;

import java.time.Instant;

/**
 * Événement publié à chaque changement de statut d'une réservation (#31), réutilisé par
 * l'annulation (#35), l'expiration automatique (#36) et le passage à « Terminée » (#37).
 * {@code previousStatus} vaut {@code null} à la création ; {@code actorId} vaut {@code null}
 * pour une transition automatique (expiration, #36). Aucun abonné en v0.2 ; les notifications
 * (v0.3) l'écouteront avec {@code @ApplicationModuleListener}.
 */
public record BookingStatusChanged(
        long bookingId,
        long clientId,
        long creatorId,
        String previousStatus,
        String newStatus,
        Long actorId,
        Instant occurredAt
) {}
