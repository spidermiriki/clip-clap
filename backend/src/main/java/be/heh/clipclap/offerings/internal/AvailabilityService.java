package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.offerings.AvailabilityApi;
import be.heh.clipclap.offerings.AvailableSlot;
import be.heh.clipclap.offerings.BookedPeriod;
import be.heh.clipclap.offerings.BookedPeriodsProvider;
import be.heh.clipclap.offerings.SlotUsageChecker;
import be.heh.clipclap.users.UserApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static be.heh.clipclap.offerings.internal.AvailabilityRepository.BRUXELLES;

/**
 * Créneaux libres, préavis et marge des créateurs (#28). Un créneau est réservable si son créateur
 * est validé et non suspendu, s'il commence après maintenant + préavis, et s'il ne touche aucune
 * réservation confirmée élargie de la marge (avant comme après).
 */
@Service
class AvailabilityService implements AvailabilityApi {

    static final Duration DUREE_MAX = Duration.ofHours(24);
    static final int PREAVIS_MAX_HEURES = 720;
    static final int MARGE_MAX_MINUTES = 480;

    // Tant que bookings ne fournit pas son implémentation, aucune période n'est occupée
    private static final BookedPeriodsProvider AUCUNE_RESERVATION = (creatorId, from, to) -> List.of();
    private static final SlotUsageChecker AUCUN_CRENEAU_RESERVE = slotId -> false;

    private final AvailabilityRepository availability;
    private final OfferingRepository offerings;
    private final CreatorProfileService creatorProfiles;
    private final UserApi userApi;
    private final ObjectProvider<BookedPeriodsProvider> bookedPeriods;
    private final ObjectProvider<SlotUsageChecker> slotUsage;
    private final Clock clock;

    AvailabilityService(AvailabilityRepository availability, OfferingRepository offerings,
                        CreatorProfileService creatorProfiles, UserApi userApi,
                        ObjectProvider<BookedPeriodsProvider> bookedPeriods,
                        ObjectProvider<SlotUsageChecker> slotUsage, Clock offeringsClock) {
        this.availability = availability;
        this.offerings = offerings;
        this.creatorProfiles = creatorProfiles;
        this.userApi = userApi;
        this.bookedPeriods = bookedPeriods;
        this.slotUsage = slotUsage;
        this.clock = offeringsClock;
    }

    // ── API publique ────────────────────────────────────────────────────────

    @Override
    public List<AvailableSlot> findAvailableSlots(long creatorUserId, LocalDate date) {
        if (!estActif(creatorUserId)) {
            return List.of();
        }
        var slots = availability.findStartingBetween(creatorUserId, debutDuJour(date), debutDuJour(date.plusDays(1)));
        return reservables(creatorUserId, slots);
    }

    @Override
    public Optional<AvailableSlot> findSlot(long slotId) {
        return availability.findById(slotId);
    }

    @Override
    public boolean isBookable(long slotId, long offeringId) {
        var slot = availability.findById(slotId).orElse(null);
        var offering = offerings.findById(offeringId).orElse(null);
        if (slot == null || offering == null || offering.creatorUserId() != slot.creatorUserId()) {
            return false;
        }
        if (Duration.between(slot.start(), slot.end()).toMinutes() < offering.durationMinutes()) {
            return false;
        }
        return estActif(slot.creatorUserId()) && !reservables(slot.creatorUserId(), List.of(slot)).isEmpty();
    }

    // ── Créneaux du créateur connecté ───────────────────────────────────────

    /** Créneaux qui commencent entre {@code from} et {@code to} inclus (dates de Bruxelles). */
    List<AvailableSlot> listMine(long userId, LocalDate from, LocalDate to) {
        requireProfile(userId);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La date de fin précède la date de début");
        }
        return availability.findStartingBetween(userId, debutDuJour(from), debutDuJour(to.plusDays(1)));
    }

    /** {@code start} et {@code end} sont des heures locales de Bruxelles. */
    @Transactional
    AvailableSlot create(long userId, LocalDateTime start, LocalDateTime end) {
        requireProfile(userId);
        if (start == null || end == null) {
            throw new IllegalArgumentException("Le début et la fin du créneau sont obligatoires");
        }
        OffsetDateTime debut = start.atZone(BRUXELLES).toOffsetDateTime();
        OffsetDateTime fin = end.atZone(BRUXELLES).toOffsetDateTime();
        if (!fin.isAfter(debut)) {
            throw new IllegalArgumentException("La fin du créneau doit suivre son début");
        }
        if (Duration.between(debut, fin).compareTo(DUREE_MAX) > 0) {
            throw new IllegalArgumentException("Un créneau ne peut pas dépasser 24 heures");
        }
        if (!debut.toInstant().isAfter(clock.instant())) {
            throw new IllegalArgumentException("Le créneau doit commencer dans le futur");
        }
        if (availability.overlaps(userId, debut, fin)) {
            throw new SlotConflictException("Ce créneau chevauche un de vos créneaux existants");
        }
        return availability.create(userId, debut, fin);
    }

    @Transactional
    void delete(long userId, long slotId) {
        var slot = availability.findById(slotId)
                .filter(s -> s.creatorUserId() == userId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));
        // Réservation En attente ou Confirmée de ce créneau (#136) : les périodes confirmées
        // ne suffisent pas, une demande en attente doit aussi bloquer la suppression
        if (slotUsage.getIfAvailable(() -> AUCUN_CRENEAU_RESERVE).hasActiveBookings(slot.id())) {
            throw new SlotConflictException("Ce créneau est réservé : il ne peut pas être supprimé");
        }
        availability.delete(slotId);
    }

    AvailabilityRepository.Settings settings(long userId) {
        requireProfile(userId);
        return availability.findSettings(userId);
    }

    AvailabilityRepository.Settings saveSettings(long userId, Integer minNoticeHours, Integer bufferMinutes) {
        requireProfile(userId);
        if (minNoticeHours == null || minNoticeHours < 0 || minNoticeHours > PREAVIS_MAX_HEURES) {
            throw new IllegalArgumentException("Le préavis doit être compris entre 0 et " + PREAVIS_MAX_HEURES + " heures");
        }
        if (bufferMinutes == null || bufferMinutes < 0 || bufferMinutes > MARGE_MAX_MINUTES) {
            throw new IllegalArgumentException("La marge doit être comprise entre 0 et " + MARGE_MAX_MINUTES + " minutes");
        }
        return availability.saveSettings(userId, new AvailabilityRepository.Settings(minNoticeHours, bufferMinutes));
    }

    // ── Règles ──────────────────────────────────────────────────────────────

    private List<AvailableSlot> reservables(long creatorId, List<AvailableSlot> slots) {
        if (slots.isEmpty()) {
            return slots;
        }
        var settings = availability.findSettings(creatorId);
        var seuil = clock.instant().plus(Duration.ofHours(settings.minNoticeHours()));
        var marge = Duration.ofMinutes(settings.bufferMinutes());
        var debut = slots.stream().map(AvailableSlot::start).min(OffsetDateTime::compareTo).orElseThrow();
        var fin = slots.stream().map(AvailableSlot::end).max(OffsetDateTime::compareTo).orElseThrow();
        var occupees = reservations(creatorId, debut.minus(marge), fin.plus(marge));
        return slots.stream()
                .filter(s -> !s.start().toInstant().isBefore(seuil))
                .filter(s -> occupees.stream().noneMatch(p -> tropProche(s, p, marge)))
                .toList();
    }

    /** Le créneau chevauche la période réservée élargie de la marge de chaque côté (se toucher est permis). */
    private static boolean tropProche(AvailableSlot slot, BookedPeriod periode, Duration marge) {
        return slot.start().isBefore(periode.end().plus(marge)) && slot.end().isAfter(periode.start().minus(marge));
    }

    private List<BookedPeriod> reservations(long creatorId, OffsetDateTime from, OffsetDateTime to) {
        return bookedPeriods.getIfAvailable(() -> AUCUNE_RESERVATION).findBookedPeriods(creatorId, from, to);
    }

    private boolean estActif(long creatorId) {
        return creatorProfiles.findByUserId(creatorId)
                .filter(p -> "VALIDATED".equals(p.validationStatus()))
                .isPresent()
                && userApi.findSuspendedIds(Set.of(creatorId)).isEmpty();
    }

    private void requireProfile(long userId) {
        if (creatorProfiles.findByUserId(userId).isEmpty()) {
            throw new CreatorProfileNotFoundException(userId);
        }
    }

    private static OffsetDateTime debutDuJour(LocalDate date) {
        return date.atStartOfDay(BRUXELLES).toOffsetDateTime();
    }
}
