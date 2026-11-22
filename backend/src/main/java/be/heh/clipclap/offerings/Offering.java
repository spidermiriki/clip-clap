package be.heh.clipclap.offerings;

import java.math.BigDecimal;
import java.util.List;

/**
 * Prestation à prix fixe d'un créateur (#27) : durée en minutes, prix de base en euros.
 * {@code categoryId} : catégorie facultative (#123), dont le nom se lit via catalog.
 */
public record Offering(
        long id,
        long creatorUserId,
        String name,
        int durationMinutes,
        BigDecimal basePrice,
        Integer categoryId,
        List<OfferingOption> options
) {}
