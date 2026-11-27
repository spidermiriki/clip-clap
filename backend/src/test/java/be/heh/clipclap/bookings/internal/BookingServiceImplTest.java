package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
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
}
