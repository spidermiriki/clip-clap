package be.heh.clipclap.offerings;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** API publique des prestations, utilisée par catalog (profil public) et bookings (réservation). */
public interface OfferingApi {

    Optional<Offering> findOffering(long offeringId);

    /** Prestations d'un créateur, de la plus ancienne à la plus récente. */
    List<Offering> findOfferingsOf(long creatorUserId);

    /**
     * Prix de la prestation avec ces options. Vide si la prestation n'existe pas ;
     * {@link IllegalArgumentException} si une option n'appartient pas à la prestation.
     */
    Optional<PriceQuote> quote(long offeringId, Set<Long> optionIds);
}
