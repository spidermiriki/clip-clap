package be.heh.clipclap.bookings;

import java.util.List;
import java.util.Map;

/**
 * Réservations reçues par le créateur connecté (#31), triées par créneau croissant (les
 * réservations sans créneau en dernier). {@code counts} contient toujours les six statuts,
 * même à 0.
 */
public record CreatorBookings(Map<String, Long> counts, List<BookingView> bookings) {}
