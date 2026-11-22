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
    void creer_inscritLaReservationEnAttenteEtSaPremiereEntreeDHistorique() {
        long client = insertUser("client@bookings.test", "sub-client-1", "CLIENT");
        long creator = insertUser("creator@bookings.test", "sub-creator-1", "CREATOR");

        Booking booking = service.creer(client, creator);

        assertThat(booking.status()).isEqualTo("EN_ATTENTE");
        BookingDetail detail = service.detail(booking.id(), client);
        assertThat(detail.historique()).hasSize(1);
        assertThat(detail.historique().get(0).actorId()).isEqualTo(client);
        assertThat(detail.historique().get(0).previousStatus()).isNull();
        assertThat(detail.historique().get(0).newStatus()).isEqualTo("EN_ATTENTE");
        assertThat(detail.historique().get(0).changedAt()).isNotNull();
    }

    @Test
    void changerStatut_transitionValide_enregistreLesQuatreValeurs() {
        long client = insertUser("client@bookings.test", "sub-client-2", "CLIENT");
        long creator = insertUser("creator@bookings.test", "sub-creator-2", "CREATOR");
        Booking booking = service.creer(client, creator);

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
        long client = insertUser("client@bookings.test", "sub-client-3", "CLIENT");
        long creator = insertUser("creator@bookings.test", "sub-creator-3", "CREATOR");
        Booking booking = service.creer(client, creator);

        assertThatThrownBy(() -> service.changerStatut(booking.id(), creator, "TERMINEE"))
                .isInstanceOf(InvalidBookingTransitionException.class);

        BookingDetail detail = service.detail(booking.id(), client);
        assertThat(detail.booking().status()).isEqualTo("EN_ATTENTE");
        assertThat(detail.historique()).hasSize(1);
    }

    @Test
    void changerStatut_transitionsSuccessives_conserveLHistoriqueDansLOrdreChronologique() {
        long client = insertUser("client@bookings.test", "sub-client-4", "CLIENT");
        long creator = insertUser("creator@bookings.test", "sub-creator-4", "CREATOR");
        Booking booking = service.creer(client, creator);

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
        long client = insertUser("client@bookings.test", "sub-client-5", "CLIENT");
        long creator = insertUser("creator@bookings.test", "sub-creator-5", "CREATOR");
        long tiers = insertUser("tiers@bookings.test", "sub-tiers-5", "CLIENT");
        Booking booking = service.creer(client, creator);

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
}
