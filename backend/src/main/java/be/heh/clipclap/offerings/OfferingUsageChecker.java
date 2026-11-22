package be.heh.clipclap.offerings;

/**
 * Indique si une prestation est liée à une réservation En attente ou Confirmée, auquel cas
 * elle ne peut pas être supprimée (#27).
 * <p>
 * Inversion de dépendance : c'est le module bookings qui fournira l'implémentation, offerings
 * ne dépendant pas de bookings. Tant qu'aucun bean n'est déclaré, offerings considère
 * qu'aucune réservation n'existe.
 */
public interface OfferingUsageChecker {

    boolean hasActiveBookings(long offeringId);
}
