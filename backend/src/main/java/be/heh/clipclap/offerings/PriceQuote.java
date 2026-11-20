package be.heh.clipclap.offerings;

import java.math.BigDecimal;
import java.util.List;

/** Prix d'une prestation avec les options choisies : {@code total} = prix de base + suppléments. */
public record PriceQuote(long offeringId, BigDecimal basePrice, List<OfferingOption> selectedOptions, BigDecimal total) {}
