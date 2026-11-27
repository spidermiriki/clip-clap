package be.heh.clipclap.offerings;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Périodes occupées par les réservations <b>actives</b> (En attente ou Confirmée, #30) d'un
 * créateur, qui se chevauchent avec {@code [from, to)} : offerings en retire les créneaux,
 * marge comprise (#28). Une demande non encore traitée doit aussi faire respecter la marge —
 * rien ne la revérifie au moment de sa confirmation (#31).
 * <p>
 * Inversion de dépendance, comme {@link OfferingUsageChecker} : c'est bookings qui fournira
 * l'implémentation. Tant qu'aucun bean n'est déclaré, aucune période n'est occupée.
 */
public interface BookedPeriodsProvider {

    List<BookedPeriod> findBookedPeriods(long creatorUserId, OffsetDateTime from, OffsetDateTime to);
}
