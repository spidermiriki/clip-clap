package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingDetail;
import be.heh.clipclap.bookings.BookingStatusChanged;
import be.heh.clipclap.bookings.BookingView;
import be.heh.clipclap.bookings.CreatorBookings;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.offerings.AvailabilityApi;
import be.heh.clipclap.offerings.OfferingApi;
import be.heh.clipclap.users.UserApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Création d'une réservation (#29), acceptation/refus (#31) et autres transitions de statut,
 * avec journalisation (#39) et publication d'un événement à chaque changement (#31, réutilisé
 * par l'annulation #35, l'expiration automatique #36 et le passage à « Terminée » #37).
 */
@Service
class BookingServiceImpl {

    /** Statuts possibles, dans l'ordre où {@link #listerPourCreateur} les affiche. */
    private static final List<String> STATUTS = List.of(
            "EN_ATTENTE", "CONFIRMEE", "REFUSEE", "ANNULEE", "EXPIREE", "TERMINEE");

    /** Transitions autorisées, statut de départ → statuts d'arrivée possibles. */
    private static final Map<String, Set<String>> TRANSITIONS_AUTORISEES = Map.of(
            "EN_ATTENTE", Set.of("CONFIRMEE", "REFUSEE", "ANNULEE", "EXPIREE"),
            "CONFIRMEE", Set.of("ANNULEE", "TERMINEE")
    );

    private final BookingRepository bookings;
    private final OfferingApi offeringApi;
    private final AvailabilityApi availabilityApi;
    private final CreatorProfileService creatorProfiles;
    private final UserApi userApi;
    private final ApplicationEventPublisher events;

    BookingServiceImpl(BookingRepository bookings, OfferingApi offeringApi, AvailabilityApi availabilityApi,
                       CreatorProfileService creatorProfiles, UserApi userApi, ApplicationEventPublisher events) {
        this.bookings = bookings;
        this.offeringApi = offeringApi;
        this.availabilityApi = availabilityApi;
        this.creatorProfiles = creatorProfiles;
        this.userApi = userApi;
        this.events = events;
    }

    @Transactional
    Booking creer(long clientId, BookingCommand command) {
        if (command == null || command.offeringId() == null || command.slotId() == null) {
            throw new IllegalArgumentException("La prestation et le créneau sont obligatoires");
        }
        var offering = offeringApi.findOffering(command.offeringId())
                .orElseThrow(() -> new OfferingNotFoundException(command.offeringId()));
        if (offering.creatorUserId() == clientId) {
            throw new SelfBookingException();
        }
        availabilityApi.findSlot(command.slotId())
                .orElseThrow(() -> new SlotNotFoundException(command.slotId()));
        if (!availabilityApi.isBookable(command.slotId(), command.offeringId())) {
            throw new SlotNotBookableException(command.slotId());
        }
        Set<Long> optionIds = command.optionIds() == null ? Set.of() : command.optionIds();
        var quote = offeringApi.quote(command.offeringId(), optionIds)
                .orElseThrow(() -> new OfferingNotFoundException(command.offeringId()));

        Booking booking;
        try {
            // uq_bookings_slot_active (#30) : si une autre réservation active a été créée sur ce
            // créneau entre le recontrôle isBookable ci-dessus et cet INSERT, la contrainte
            // d'unicité en base l'empêche — recontrôle synchrone et contrainte couvrent deux
            // fenêtres différentes (avant/pendant l'écriture), la seconde seule est fiable sous
            // concurrence réelle (scénario 21).
            booking = bookings.create(clientId, offering.creatorUserId(),
                    command.offeringId(), command.slotId(), quote.total());
        } catch (DuplicateKeyException e) {
            throw new SlotNotBookableException(command.slotId());
        }
        bookings.insertHistoryEntry(booking.id(), clientId, null, booking.status());
        events.publishEvent(new BookingStatusChanged(booking.id(), clientId, offering.creatorUserId(),
                null, booking.status(), clientId, booking.createdAt()));
        return booking;
    }

    @Transactional
    Booking changerStatut(long bookingId, long actorId, String nouveauStatut) {
        return changerStatut(bookingId, actorId, nouveauStatut, null);
    }

    @Transactional
    Booking changerStatut(long bookingId, long actorId, String nouveauStatut, String refusalMessage) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        String statutActuel = booking.status();

        if (!TRANSITIONS_AUTORISEES.getOrDefault(statutActuel, Set.of()).contains(nouveauStatut)) {
            throw new InvalidBookingTransitionException(statutActuel, nouveauStatut);
        }

        // La mise à jour porte elle-même la condition de statut (pas de verrou explicite) : si une
        // requête concurrente a déjà changé le statut entre notre lecture et cette écriture, aucune
        // ligne n'est modifiée et aucun historique n'est écrit pour cette transition.
        if (!bookings.updateStatusSiActuel(bookingId, statutActuel, nouveauStatut, refusalMessage)) {
            String statutReel = bookings.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException(bookingId))
                    .status();
            throw new InvalidBookingTransitionException(statutReel, nouveauStatut);
        }

        bookings.insertHistoryEntry(bookingId, actorId, statutActuel, nouveauStatut);
        events.publishEvent(new BookingStatusChanged(bookingId, booking.clientId(), booking.creatorId(),
                statutActuel, nouveauStatut, actorId, Instant.now()));
        return bookings.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    /** Accepter (#31) : seul le créateur concerné peut confirmer une demande En attente. */
    @Transactional
    BookingView accepter(long bookingId, long actorId) {
        Booking avant = requireCreateur(bookingId, actorId);
        Booking booking;
        try {
            booking = changerStatut(bookingId, actorId, "CONFIRMEE");
        } catch (DuplicateKeyException e) {
            // uq_bookings_slot_active : changerStatut ne fait qu'un UPDATE sur la ligne déjà
            // active pour ce créneau, jamais un INSERT — inatteignable avec le schéma actuel,
            // gardé en garde-fou défensif (revue de #31 avec @spidermiriki).
            throw new SlotNotBookableException(avant.slotId());
        }
        return toViews(List.of(booking)).get(0);
    }

    /** Refuser (#31) : message facultatif, 500 caractères au plus, visible du client (#34). */
    @Transactional
    BookingView refuser(long bookingId, long actorId, RefuseCommand command) {
        requireCreateur(bookingId, actorId);
        String message = command == null ? null : command.message();
        if (message != null && message.length() > 500) {
            throw new IllegalArgumentException("Le message de refus dépasse 500 caractères");
        }
        Booking booking = changerStatut(bookingId, actorId, "REFUSEE", message);
        return toViews(List.of(booking)).get(0);
    }

    /** Réservations reçues par le créateur connecté (#31), avec le compte de chaque statut. */
    CreatorBookings listerPourCreateur(long creatorId) {
        List<Booking> mine = bookings.findByCreator(creatorId);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String statut : STATUTS) {
            counts.put(statut, 0L);
        }
        for (Booking booking : mine) {
            counts.merge(booking.status(), 1L, Long::sum);
        }
        return new CreatorBookings(counts, toViews(mine));
    }

    BookingDetail detail(long bookingId, long requesterId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (requesterId != booking.clientId() && requesterId != booking.creatorId()) {
            throw new BookingAccessDeniedException();
        }

        return new BookingDetail(booking, bookings.findHistory(bookingId));
    }

    private Booking requireCreateur(long bookingId, long actorId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (booking.creatorId() != actorId) {
            throw new BookingAccessDeniedException();
        }
        return booking;
    }

    /**
     * Vue enrichie (#31), par lot : un seul appel à {@link UserApi#findEmails} pour tous les
     * clients de la liste, un nom de créateur mis en cache par identifiant déjà résolu.
     */
    private List<BookingView> toViews(List<Booking> mine) {
        if (mine.isEmpty()) {
            return List.of();
        }
        Set<Long> clientIds = mine.stream().map(Booking::clientId).collect(Collectors.toSet());
        Map<Long, String> emails = userApi.findEmails(clientIds);
        Map<Long, String> displayNames = new HashMap<>();
        return mine.stream().map(booking -> toView(booking, emails, displayNames)).toList();
    }

    private BookingView toView(Booking booking, Map<Long, String> emails, Map<Long, String> displayNames) {
        BookingView.OfferingSummary offering = booking.offeringId() == null ? null
                : offeringApi.findOffering(booking.offeringId())
                        .map(o -> new BookingView.OfferingSummary(o.id(), o.name()))
                        .orElse(null);
        BookingView.SlotSummary slot = booking.slotId() == null ? null
                : availabilityApi.findSlot(booking.slotId())
                        .map(s -> new BookingView.SlotSummary(s.start(), s.end()))
                        .orElse(null);
        var client = new BookingView.ClientSummary(booking.clientId(), emails.get(booking.clientId()));
        String displayName = displayNames.computeIfAbsent(booking.creatorId(),
                id -> creatorProfiles.findByUserId(id).map(CreatorProfile::displayName).orElse(null));
        var creator = new BookingView.CreatorSummary(booking.creatorId(), displayName);
        return new BookingView(booking.id(), booking.status(), booking.totalPrice(), booking.createdAt(),
                offering, slot, client, creator, booking.refusalMessage());
    }
}
