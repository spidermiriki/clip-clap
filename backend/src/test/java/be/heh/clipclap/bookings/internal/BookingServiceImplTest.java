package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingDetail;
import be.heh.clipclap.bookings.BookingStatusChanged;
import be.heh.clipclap.bookings.BookingView;
import be.heh.clipclap.bookings.CreatorBookings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RecordApplicationEvents
@Transactional
@Import(TestcontainersConfiguration.class)
class BookingServiceImplTest {

    @Autowired
    BookingServiceImpl service;

    @Autowired
    JdbcClient jdbc;

    @Test
    void creer_prestationEtCreneauValides_inscritLaReservationEnAttenteAvecLeTotalEtSaPremiereEntreeDHistorique() {
        long client = insertUser("client@bookings.test", "sub-client-1", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-1");
        long offering = insertOffering(creator, 60, "100.00");
        long option = insertOption(offering, "20.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of(option)));

        assertThat(booking.status()).isEqualTo("EN_ATTENTE");
        assertThat(booking.clientId()).isEqualTo(client);
        assertThat(booking.creatorId()).isEqualTo(creator);
        assertThat(booking.offeringId()).isEqualTo(offering);
        assertThat(booking.slotId()).isEqualTo(slot);
        assertThat(booking.totalPrice()).isEqualByComparingTo(new BigDecimal("120.00"));

        BookingDetail detail = service.detail(booking.id(), client);
        assertThat(detail.historique()).hasSize(1);
        assertThat(detail.historique().get(0).actorId()).isEqualTo(client);
        assertThat(detail.historique().get(0).previousStatus()).isNull();
        assertThat(detail.historique().get(0).newStatus()).isEqualTo("EN_ATTENTE");
    }

    @Test
    void creer_createurReserveSaProprePrestation_leveSelfBookingException() {
        long creator = insertCreator("createur@bookings.test", "sub-creator-2");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        assertThatThrownBy(() -> service.creer(creator, new BookingCommand(offering, slot, Set.of())))
                .isInstanceOf(SelfBookingException.class);
    }

    @Test
    void creer_creneauTropCourtPourLaPrestation_leveSlotNotBookableException() {
        long client = insertUser("client@bookings.test", "sub-client-3", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-3");
        long offering = insertOffering(creator, 90, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T09:30+00:00");

        assertThatThrownBy(() -> service.creer(client, new BookingCommand(offering, slot, Set.of())))
                .isInstanceOf(SlotNotBookableException.class);
    }

    @Test
    void creer_creneauDejaReserveParUnAutreClient_leveSlotNotBookableException() {
        long creator = insertCreator("creator@bookings.test", "sub-creator-3b");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        long premierClient = insertUser("premier@bookings.test", "sub-premier-3b", "CLIENT");
        long secondClient = insertUser("second@bookings.test", "sub-second-3b", "CLIENT");
        service.creer(premierClient, new BookingCommand(offering, slot, Set.of()));

        // Depuis que findActivePeriods (#30, revue de #139) compte aussi EN_ATTENTE, isBookable()
        // refuse déjà ce même créneau de façon synchrone (occupé par la première demande) ; seule
        // une vraie course (deux INSERT concurrents, ni l'un ni l'autre encore visible par
        // isBookable au moment de leur propre lecture) exercerait uq_bookings_slot_active
        // elle-même — couvert séparément par BookingServiceImplConcurrenceTest, impossible à
        // provoquer de façon fiable ici avec une vraie base dans un test à une seule transaction.
        assertThatThrownBy(() -> service.creer(secondClient, new BookingCommand(offering, slot, Set.of())))
                .isInstanceOf(SlotNotBookableException.class);
    }

    @Test
    void creer_demandeEnAttenteSurUnCreneauVoisin_bloqueLeVoisinDansLaMarge() {
        // Revue de #139 : avant ce correctif, findActivePeriods (alors findConfirmedPeriods) ne
        // comptait que CONFIRMEE — une demande En attente seule ne faisait pas respecter la marge.
        long creator = insertCreator("creator@bookings.test", "sub-creator-marge");
        setSettings(creator, 0, 30);
        long offering = insertOffering(creator, 60, "100.00");
        long slotProche = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        // Commence 15 min après la fin du premier : à l'intérieur de la marge de 30 min
        long slotVoisin = insertSlot(creator, "2030-01-01T10:15+00:00", "2030-01-01T11:15+00:00");
        long premierClient = insertUser("premier@bookings.test", "sub-premier-marge", "CLIENT");
        long secondClient = insertUser("second@bookings.test", "sub-second-marge", "CLIENT");

        service.creer(premierClient, new BookingCommand(offering, slotProche, Set.of())); // EN_ATTENTE seulement

        assertThatThrownBy(() -> service.creer(secondClient, new BookingCommand(offering, slotVoisin, Set.of())))
                .isInstanceOf(SlotNotBookableException.class);
    }

    @Test
    void creer_creneauLibereApresRefus_permetUneNouvelleReservation() {
        long creator = insertCreator("creator@bookings.test", "sub-creator-3c");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        long premierClient = insertUser("premier@bookings.test", "sub-premier-3c", "CLIENT");
        long secondClient = insertUser("second@bookings.test", "sub-second-3c", "CLIENT");
        Booking premiere = service.creer(premierClient, new BookingCommand(offering, slot, Set.of()));
        service.changerStatut(premiere.id(), creator, "REFUSEE");

        Booking seconde = service.creer(secondClient, new BookingCommand(offering, slot, Set.of()));

        assertThat(seconde.status()).isEqualTo("EN_ATTENTE");
    }

    @Test
    void creer_prestationInconnue_leveOfferingNotFoundException() {
        long client = insertUser("client@bookings.test", "sub-client-4", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-4");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        assertThatThrownBy(() -> service.creer(client, new BookingCommand(999_999L, slot, Set.of())))
                .isInstanceOf(OfferingNotFoundException.class);
    }

    @Test
    void creer_creneauInconnu_leveSlotNotFoundException() {
        long client = insertUser("client@bookings.test", "sub-client-5", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-5");
        long offering = insertOffering(creator, 60, "100.00");

        assertThatThrownBy(() -> service.creer(client, new BookingCommand(offering, 999_999L, Set.of())))
                .isInstanceOf(SlotNotFoundException.class);
    }

    @Test
    void creer_offeringIdOuSlotIdManquant_leveIllegalArgumentException() {
        long client = insertUser("client@bookings.test", "sub-client-5b", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-5b");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        assertThatThrownBy(() -> service.creer(client, new BookingCommand(null, slot, Set.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.creer(client, new BookingCommand(offering, null, Set.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creer_optionInconnuePourLaPrestation_leveIllegalArgumentException() {
        long client = insertUser("client@bookings.test", "sub-client-6", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-6");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        assertThatThrownBy(() -> service.creer(client, new BookingCommand(offering, slot, Set.of(999_999L))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changerStatut_transitionValide_enregistreLesQuatreValeurs() {
        long client = insertUser("client@bookings.test", "sub-client-7", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-7");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        Booking confirmee = service.changerStatut(booking.id(), creator, "CONFIRMEE");

        assertThat(confirmee.status()).isEqualTo("CONFIRMEE");
        var historique = service.detail(booking.id(), client).historique();
        assertThat(historique).hasSize(2);
        var transition = historique.get(1);
        assertThat(transition.actorId()).isEqualTo(creator);
        assertThat(transition.previousStatus()).isEqualTo("EN_ATTENTE");
        assertThat(transition.newStatus()).isEqualTo("CONFIRMEE");
        assertThat(transition.changedAt()).isNotNull();
    }

    @Test
    void changerStatut_transitionInvalide_neModifieNiLeStatutNiLHistorique() {
        long client = insertUser("client@bookings.test", "sub-client-8", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-8");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        assertThatThrownBy(() -> service.changerStatut(booking.id(), creator, "TERMINEE"))
                .isInstanceOf(InvalidBookingTransitionException.class);

        BookingDetail detail = service.detail(booking.id(), client);
        assertThat(detail.booking().status()).isEqualTo("EN_ATTENTE");
        assertThat(detail.historique()).hasSize(1);
    }

    @Test
    void changerStatut_transitionsSuccessives_conserveLHistoriqueDansLOrdreChronologique() {
        long client = insertUser("client@bookings.test", "sub-client-9", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-9");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        service.changerStatut(booking.id(), creator, "CONFIRMEE");
        service.changerStatut(booking.id(), creator, "TERMINEE");

        var historique = service.detail(booking.id(), client).historique();
        assertThat(historique).hasSize(3);
        assertThat(historique.get(0).newStatus()).isEqualTo("EN_ATTENTE");
        assertThat(historique.get(1).newStatus()).isEqualTo("CONFIRMEE");
        assertThat(historique.get(2).newStatus()).isEqualTo("TERMINEE");
    }

    @Test
    void detail_demandeParUnTiers_refuseLAcces() {
        long client = insertUser("client@bookings.test", "sub-client-10", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-10");
        long tiers = insertUser("tiers@bookings.test", "sub-tiers-10", "CLIENT");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        assertThatThrownBy(() -> service.detail(booking.id(), tiers))
                .isInstanceOf(BookingAccessDeniedException.class);
    }

    @Test
    void detail_reservationInconnue_renvoieBookingNotFound() {
        assertThatThrownBy(() -> service.detail(999_999L, 1L))
                .isInstanceOf(BookingNotFoundException.class);
    }

    // ── Accepter / refuser (#31) ────────────────────────────────────────────

    @Test
    void accepter_parLeCreateurConcerne_confirmeEtRenvoieLaVue() {
        long client = insertUser("client@bookings.test", "sub-client-accept", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-accept");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        BookingView vue = service.accepter(booking.id(), creator);

        assertThat(vue.status()).isEqualTo("CONFIRMEE");
        assertThat(vue.client().email()).isEqualTo("client@bookings.test");
        assertThat(vue.offering().name()).isEqualTo("Prestation");
        assertThat(vue.slot().start()).isEqualTo(OffsetDateTime.parse("2030-01-01T09:00+00:00"));
    }

    @Test
    void accepter_parUnTiers_leveBookingAccessDeniedException() {
        long client = insertUser("client@bookings.test", "sub-client-accept2", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-accept2");
        long tiers = insertCreator("tiers@bookings.test", "sub-tiers-accept2");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        assertThatThrownBy(() -> service.accepter(booking.id(), tiers))
                .isInstanceOf(BookingAccessDeniedException.class);
    }

    @Test
    void accepter_reservationPasEnAttente_leveInvalidBookingTransitionException() {
        long client = insertUser("client@bookings.test", "sub-client-accept3", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-accept3");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));
        service.changerStatut(booking.id(), creator, "REFUSEE");

        assertThatThrownBy(() -> service.accepter(booking.id(), creator))
                .isInstanceOf(InvalidBookingTransitionException.class);
    }

    @Test
    void refuser_parLeCreateurConcerne_refuseEtEnregistreLeMessage() {
        long client = insertUser("client@bookings.test", "sub-client-refuse", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-refuse");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        BookingView vue = service.refuser(booking.id(), creator, new RefuseCommand("Indisponible ce jour-là"));

        assertThat(vue.status()).isEqualTo("REFUSEE");
        assertThat(vue.refusalMessage()).isEqualTo("Indisponible ce jour-là");
    }

    @Test
    void refuser_sansMessage_refuseSansMessage() {
        long client = insertUser("client@bookings.test", "sub-client-refuse2", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-refuse2");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        BookingView vue = service.refuser(booking.id(), creator, null);

        assertThat(vue.status()).isEqualTo("REFUSEE");
        assertThat(vue.refusalMessage()).isNull();
    }

    @Test
    void refuser_messageTropLong_leveIllegalArgumentException() {
        long client = insertUser("client@bookings.test", "sub-client-refuse3", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-refuse3");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));
        String messageTropLong = "x".repeat(501);

        assertThatThrownBy(() -> service.refuser(booking.id(), creator, new RefuseCommand(messageTropLong)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuser_parUnTiers_leveBookingAccessDeniedException() {
        long client = insertUser("client@bookings.test", "sub-client-refuse4", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-refuse4");
        long tiers = insertCreator("tiers@bookings.test", "sub-tiers-refuse4");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        assertThatThrownBy(() -> service.refuser(booking.id(), tiers, null))
                .isInstanceOf(BookingAccessDeniedException.class);
    }

    // ── Tableau de bord créateur (#31) ──────────────────────────────────────

    @Test
    void listerPourCreateur_compteTousLesStatutsEtTrieParCreneauCroissant() {
        long creator = insertCreator("creator@bookings.test", "sub-creator-liste");
        long offering = insertOffering(creator, 60, "100.00");
        long slotTard = insertSlot(creator, "2030-01-02T09:00+00:00", "2030-01-02T10:00+00:00");
        long slotTot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        long client1 = insertUser("client1@bookings.test", "sub-client-liste1", "CLIENT");
        long client2 = insertUser("client2@bookings.test", "sub-client-liste2", "CLIENT");
        Booking enregistreTard = service.creer(client1, new BookingCommand(offering, slotTard, Set.of()));
        Booking enregistreTot = service.creer(client2, new BookingCommand(offering, slotTot, Set.of()));

        CreatorBookings mine = service.listerPourCreateur(creator);

        assertThat(mine.counts().keySet())
                .containsExactlyInAnyOrder("EN_ATTENTE", "CONFIRMEE", "REFUSEE", "ANNULEE", "EXPIREE", "TERMINEE");
        assertThat(mine.counts().get("EN_ATTENTE")).isEqualTo(2L);
        assertThat(mine.counts().get("CONFIRMEE")).isEqualTo(0L);
        assertThat(mine.bookings()).extracting(BookingView::id)
                .containsExactly(enregistreTot.id(), enregistreTard.id());
        assertThat(mine.bookings().get(0).creator().displayName()).isEqualTo("creator@bookings.test");
    }

    @Test
    void listerPourCreateur_reservationSansCreneauApparaitEnDernier() {
        long creator = insertCreator("creator@bookings.test", "sub-creator-liste2");
        long offering = insertOffering(creator, 60, "100.00");
        long slotConserve = insertSlot(creator, "2030-06-01T09:00+00:00", "2030-06-01T10:00+00:00");
        // Antérieur à slotConserve, mais supprimé ensuite : doit tout de même finir en dernier
        long slotASupprimer = insertSlot(creator, "2029-01-01T09:00+00:00", "2029-01-01T10:00+00:00");
        long client1 = insertUser("client1@bookings.test", "sub-client-liste3", "CLIENT");
        long client2 = insertUser("client2@bookings.test", "sub-client-liste4", "CLIENT");
        Booking avecCreneau = service.creer(client1, new BookingCommand(offering, slotConserve, Set.of()));
        Booking sansCreneauBientot = service.creer(client2, new BookingCommand(offering, slotASupprimer, Set.of()));
        service.changerStatut(sansCreneauBientot.id(), creator, "REFUSEE");
        jdbc.sql("DELETE FROM availability_slots WHERE id = :id").param("id", slotASupprimer).update();

        CreatorBookings mine = service.listerPourCreateur(creator);

        assertThat(mine.bookings()).extracting(BookingView::id)
                .containsExactly(avecCreneau.id(), sansCreneauBientot.id());
        assertThat(mine.bookings().get(1).slot()).isNull();
        assertThat(mine.bookings().get(1).offering()).isNotNull();
    }

    // ── Événement publié à chaque changement de statut (#31) ────────────────

    @Test
    void creer_publieUnEvenementBookingStatusChanged(ApplicationEvents events) {
        long client = insertUser("client@bookings.test", "sub-client-event1", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-event1");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");

        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        var evenement = events.stream(BookingStatusChanged.class).findFirst().orElseThrow();
        assertThat(evenement.bookingId()).isEqualTo(booking.id());
        assertThat(evenement.clientId()).isEqualTo(client);
        assertThat(evenement.creatorId()).isEqualTo(creator);
        assertThat(evenement.previousStatus()).isNull();
        assertThat(evenement.newStatus()).isEqualTo("EN_ATTENTE");
        assertThat(evenement.actorId()).isEqualTo(client);
        assertThat(evenement.occurredAt()).isEqualTo(booking.createdAt());
    }

    @Test
    void changerStatut_publieUnEvenementAvecLActeurEtLesDeuxStatuts(ApplicationEvents events) {
        long client = insertUser("client@bookings.test", "sub-client-event2", "CLIENT");
        long creator = insertCreator("creator@bookings.test", "sub-creator-event2");
        long offering = insertOffering(creator, 60, "100.00");
        long slot = insertSlot(creator, "2030-01-01T09:00+00:00", "2030-01-01T10:00+00:00");
        Booking booking = service.creer(client, new BookingCommand(offering, slot, Set.of()));

        service.changerStatut(booking.id(), creator, "CONFIRMEE");

        var transition = events.stream(BookingStatusChanged.class)
                .filter(e -> "CONFIRMEE".equals(e.newStatus()))
                .findFirst().orElseThrow();
        assertThat(transition.previousStatus()).isEqualTo("EN_ATTENTE");
        assertThat(transition.actorId()).isEqualTo(creator);
    }

    private long insertUser(String email, String sub, String role) {
        return jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, :role) RETURNING id")
                .param("email", email).param("sub", sub).param("role", role)
                .query(Long.class).single();
    }

    private long insertCreator(String email, String sub) {
        long id = insertUser(email, sub, "CREATOR");
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name, validation_status) VALUES (:id, :n, 'VALIDATED')")
                .param("id", id).param("n", email)
                .update();
        return id;
    }

    private long insertOffering(long creatorId, int durationMinutes, String basePrice) {
        return jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                VALUES (:c, 'Prestation', :d, :p) RETURNING id
                """)
                .param("c", creatorId).param("d", durationMinutes).param("p", new BigDecimal(basePrice))
                .query(Long.class).single();
    }

    private long insertOption(long offeringId, String extraPrice) {
        return jdbc.sql("""
                INSERT INTO offering_options (offering_id, name, extra_price)
                VALUES (:o, 'Option', :p) RETURNING id
                """)
                .param("o", offeringId).param("p", new BigDecimal(extraPrice))
                .query(Long.class).single();
    }

    private long insertSlot(long creatorId, String startsAt, String endsAt) {
        return jdbc.sql("""
                INSERT INTO availability_slots (creator_id, starts_at, ends_at)
                VALUES (:c, :d, :f) RETURNING id
                """)
                .param("c", creatorId)
                .param("d", OffsetDateTime.parse(startsAt))
                .param("f", OffsetDateTime.parse(endsAt))
                .query(Long.class).single();
    }

    private void setSettings(long creatorId, int preavisHeures, int margeMinutes) {
        jdbc.sql("INSERT INTO scheduling_settings (creator_id, min_notice_hours, buffer_minutes) VALUES (:c, :p, :m)")
                .param("c", creatorId).param("p", preavisHeures).param("m", margeMinutes)
                .update();
    }
}
