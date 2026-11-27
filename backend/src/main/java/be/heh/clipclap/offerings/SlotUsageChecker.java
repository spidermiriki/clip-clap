package be.heh.clipclap.offerings;

/**
 * Indique si un créneau est lié à une réservation En attente ou Confirmée, auquel cas il ne peut
 * pas être supprimé (#136).
 * <p>
 * Inversion de dépendance, comme {@link OfferingUsageChecker} : c'est bookings qui fournit
 * l'implémentation. Tant qu'aucun bean n'est déclaré, offerings considère qu'aucune réservation
 * n'existe.
 */
public interface SlotUsageChecker {

    boolean hasActiveBookings(long slotId);
}
