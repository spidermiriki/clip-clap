package be.heh.clipclap.offerings;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Périodes occupées par les réservations <b>confirmées</b> d'un créateur, qui se chevauchent avec
 * {@code [from, to)} : offerings en retire les créneaux, marge comprise (#28).
 * <p>
 * Inversion de dépendance, comme {@link OfferingUsageChecker} : c'est bookings qui fournira
 * l'implémentation. Tant qu'aucun bean n'est déclaré, aucune période n'est occupée.
 */
public interface BookedPeriodsProvider {

    List<BookedPeriod> findBookedPeriods(long creatorUserId, OffsetDateTime from, OffsetDateTime to);
}
