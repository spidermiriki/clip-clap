package be.heh.clipclap.offerings;

import java.math.BigDecimal;

/** Option payante d'une prestation (ex. drone, retouches avancées), avec son supplément en euros. */
public record OfferingOption(long id, String name, BigDecimal extraPrice) {}
