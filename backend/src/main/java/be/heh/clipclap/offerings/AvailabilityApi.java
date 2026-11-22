package be.heh.clipclap.offerings;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Disponibilités des créateurs (#28), utilisées par bookings pour la réservation (contrat de #29). */
public interface AvailabilityApi {

    /**
     * Créneaux réservables d'un créateur qui commencent ce jour-là (Europe/Brussels), dans l'ordre :
     * préavis et marge appliqués. Vide si le créateur n'est pas validé ou est suspendu.
     */
    List<AvailableSlot> findAvailableSlots(long creatorUserId, LocalDate date);

    /** Le créneau tel que défini par le créateur, réservable ou non. */
    Optional<AvailableSlot> findSlot(long slotId);

    /**
     * Recontrôle au moment de réserver : créateur actif, préavis, marge, prestation du même
     * créateur, et créneau au moins aussi long que la durée de la prestation.
     */
    boolean isBookable(long slotId, long offeringId);
}
