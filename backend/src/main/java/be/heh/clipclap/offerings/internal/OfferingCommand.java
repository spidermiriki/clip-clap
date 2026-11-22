package be.heh.clipclap.offerings.internal;

import java.math.BigDecimal;
import java.util.List;

/** Corps de création ou de modification d'une prestation ; la liste d'options remplace l'existante. */
record OfferingCommand(String name, Integer durationMinutes, BigDecimal basePrice, List<OptionCommand> options) {

    record OptionCommand(String name, BigDecimal extraPrice) {}
}
