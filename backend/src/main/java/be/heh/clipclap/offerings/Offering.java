package be.heh.clipclap.offerings;

import java.math.BigDecimal;
import java.util.List;

/** Prestation à prix fixe d'un créateur (#27) : durée en minutes, prix de base en euros. */
public record Offering(
        long id,
        long creatorUserId,
        String name,
        int durationMinutes,
        BigDecimal basePrice,
        List<OfferingOption> options
) {}
