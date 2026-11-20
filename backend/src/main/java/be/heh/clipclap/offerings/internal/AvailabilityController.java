package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.offerings.AvailableSlot;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static be.heh.clipclap.offerings.internal.AvailabilityRepository.BRUXELLES;

/**
 * Disponibilités (#28) : créneaux libres, préavis et marge du créateur connecté, et créneaux
 * réservables d'un créateur pour un client. Les dates et heures saisies sont celles de Bruxelles.
 */
@RestController
@RequestMapping("/api/offerings")
class AvailabilityController {

    record SlotCommand(LocalDateTime start, LocalDateTime end) {}

    record SettingsCommand(Integer minNoticeHours, Integer bufferMinutes) {}

    private final AvailabilityService availabilityService;
    private final OfferingRepository offerings;
    private final CurrentUser currentUser;

    AvailabilityController(AvailabilityService availabilityService, OfferingRepository offerings, CurrentUser currentUser) {
        this.availabilityService = availabilityService;
        this.offerings = offerings;
        this.currentUser = currentUser;
    }

    // ── Créateur connecté ───────────────────────────────────────────────────

    /** Par défaut, les 30 prochains jours. */
    @GetMapping("/me/slots")
    List<AvailableSlot> listMine(Authentication auth,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate debut = from != null ? from : LocalDate.now(BRUXELLES);
        LocalDate fin = to != null ? to : debut.plusDays(30);
        return availabilityService.listMine(currentUser.id(auth), debut, fin);
    }

    @PostMapping("/me/slots")
    @ResponseStatus(HttpStatus.CREATED)
    AvailableSlot create(Authentication auth, @RequestBody SlotCommand command) {
        return availabilityService.create(currentUser.id(auth), command.start(), command.end());
    }

    @DeleteMapping("/me/slots/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth, @PathVariable long slotId) {
        availabilityService.delete(currentUser.id(auth), slotId);
    }

    @GetMapping("/me/settings")
    AvailabilityRepository.Settings settings(Authentication auth) {
        return availabilityService.settings(currentUser.id(auth));
    }

    @PutMapping("/me/settings")
    AvailabilityRepository.Settings saveSettings(Authentication auth, @RequestBody SettingsCommand command) {
        return availabilityService.saveSettings(currentUser.id(auth), command.minNoticeHours(), command.bufferMinutes());
    }

    // ── Client : créneaux réservables d'un créateur ─────────────────────────

    /** Avec {@code offeringId}, seuls les créneaux assez longs pour cette prestation du créateur. */
    @GetMapping("/creators/{creatorId}/slots")
    List<AvailableSlot> available(@PathVariable long creatorId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam(required = false) Long offeringId) {
        var slots = availabilityService.findAvailableSlots(creatorId, date);
        if (offeringId == null) {
            return slots;
        }
        var offering = offerings.findById(offeringId)
                .filter(o -> o.creatorUserId() == creatorId)
                .orElseThrow(() -> new OfferingNotFoundException(offeringId));
        return slots.stream()
                .filter(s -> Duration.between(s.start(), s.end()).toMinutes() >= offering.durationMinutes())
                .toList();
    }
}
