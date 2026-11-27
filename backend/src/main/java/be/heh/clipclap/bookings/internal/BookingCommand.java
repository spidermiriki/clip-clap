package be.heh.clipclap.bookings.internal;

import java.util.Set;

/** Corps de création d'une réservation : prestation, créneau et options choisies (#29). */
record BookingCommand(Long offeringId, Long slotId, Set<Long> optionIds) {}
