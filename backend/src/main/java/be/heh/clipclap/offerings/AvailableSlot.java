package be.heh.clipclap.offerings;

import java.time.OffsetDateTime;

/** Créneau libre défini par un créateur (#28), à l'heure de Bruxelles. */
public record AvailableSlot(long id, long creatorUserId, OffsetDateTime start, OffsetDateTime end) {}
