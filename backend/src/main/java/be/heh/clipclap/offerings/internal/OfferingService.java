package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.offerings.Offering;
import be.heh.clipclap.offerings.OfferingApi;
import be.heh.clipclap.offerings.OfferingOption;
import be.heh.clipclap.offerings.OfferingUsageChecker;
import be.heh.clipclap.offerings.PriceQuote;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
class OfferingService implements OfferingApi {

    static final int NAME_MAX_LENGTH = 100;
    static final int DURATION_MAX_MINUTES = 24 * 60;
    // Limite de NUMERIC(10,2)
    static final BigDecimal PRICE_MAX = new BigDecimal("99999999.99");

    // Tant que bookings ne fournit pas son implémentation, aucune réservation n'existe
    private static final OfferingUsageChecker AUCUNE_RESERVATION = offeringId -> false;

    private final OfferingRepository offerings;
    private final CreatorProfileService creatorProfiles;
    private final ObjectProvider<OfferingUsageChecker> usageChecker;

    OfferingService(OfferingRepository offerings, CreatorProfileService creatorProfiles,
                    ObjectProvider<OfferingUsageChecker> usageChecker) {
        this.offerings = offerings;
        this.creatorProfiles = creatorProfiles;
        this.usageChecker = usageChecker;
    }

    // ── API publique ────────────────────────────────────────────────────────

    @Override
    public Optional<Offering> findOffering(long offeringId) {
        return offerings.findById(offeringId);
    }

    @Override
    public List<Offering> findOfferingsOf(long creatorUserId) {
        return offerings.findByCreator(creatorUserId);
    }

    @Override
    public Optional<PriceQuote> quote(long offeringId, Set<Long> optionIds) {
        return offerings.findById(offeringId).map(offering -> {
            List<OfferingOption> selected = offering.options().stream()
                    .filter(o -> optionIds.contains(o.id()))
                    .toList();
            if (selected.size() != optionIds.size()) {
                throw new IllegalArgumentException("Option inconnue pour la prestation " + offeringId);
            }
            BigDecimal total = selected.stream()
                    .map(OfferingOption::extraPrice)
                    .reduce(offering.basePrice(), BigDecimal::add);
            return new PriceQuote(offeringId, offering.basePrice(), selected, total);
        });
    }

    // ── Prestations du créateur connecté ────────────────────────────────────

    List<Offering> listMine(long userId) {
        requireProfile(userId);
        return offerings.findByCreator(userId);
    }

    @Transactional
    Offering create(long userId, OfferingCommand command) {
        requireProfile(userId);
        var valid = validate(command);
        long id = offerings.create(userId, valid.name(), valid.durationMinutes(), valid.basePrice());
        offerings.replaceOptions(id, valid.options());
        return offerings.findById(id).orElseThrow();
    }

    @Transactional
    Offering update(long userId, long offeringId, OfferingCommand command) {
        var valid = validate(command);
        if (!offerings.update(offeringId, userId, valid.name(), valid.durationMinutes(), valid.basePrice())) {
            throw new OfferingNotFoundException(offeringId);
        }
        offerings.replaceOptions(offeringId, valid.options());
        return offerings.findById(offeringId).orElseThrow();
    }

    @Transactional
    void delete(long userId, long offeringId) {
        if (!offerings.isOwnedBy(offeringId, userId)) {
            throw new OfferingNotFoundException(offeringId);
        }
        if (usageChecker.getIfAvailable(() -> AUCUNE_RESERVATION).hasActiveBookings(offeringId)) {
            throw new OfferingInUseException();
        }
        offerings.delete(offeringId);
    }

    private void requireProfile(long userId) {
        if (creatorProfiles.findByUserId(userId).isEmpty()) {
            throw new CreatorProfileNotFoundException(userId);
        }
    }

    // ── Validation ──────────────────────────────────────────────────────────

    private OfferingCommand validate(OfferingCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Corps de la requête manquant");
        }
        String name = validName(command.name(), "Le nom de la prestation");
        Integer duration = command.durationMinutes();
        if (duration == null || duration < 1 || duration > DURATION_MAX_MINUTES) {
            throw new IllegalArgumentException("La durée doit être comprise entre 1 et " + DURATION_MAX_MINUTES + " minutes");
        }
        BigDecimal price = validPrice(command.basePrice(), "Le prix de base");
        List<OfferingCommand.OptionCommand> options = command.options() == null ? List.of() : command.options().stream()
                .map(o -> {
                    if (o == null) {
                        throw new IllegalArgumentException("Option vide");
                    }
                    return new OfferingCommand.OptionCommand(
                            validName(o.name(), "Le nom de l'option"), validPrice(o.extraPrice(), "Le supplément"));
                })
                .toList();
        return new OfferingCommand(name, duration, price, options);
    }

    private static String validName(String name, String label) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " est obligatoire");
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(label + " dépasse " + NAME_MAX_LENGTH + " caractères");
        }
        return trimmed;
    }

    private static BigDecimal validPrice(BigDecimal price, String label) {
        if (price == null) {
            throw new IllegalArgumentException(label + " est obligatoire");
        }
        if (price.signum() < 0 || price.compareTo(PRICE_MAX) > 0) {
            throw new IllegalArgumentException(label + " doit être compris entre 0 et " + PRICE_MAX + " €");
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(label + " a au plus deux décimales");
        }
        return price;
    }
}
