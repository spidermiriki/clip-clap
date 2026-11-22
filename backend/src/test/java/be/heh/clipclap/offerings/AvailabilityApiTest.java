package be.heh.clipclap.offerings;

import be.heh.clipclap.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Règles de disponibilité (#28) vues par bookings (contrat de #29). Horloge figée au jeudi
 * 25 mars 2027, 10 h UTC : l'heure d'été commence à Bruxelles le dimanche 28 mars.
 */
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class AvailabilityApiTest {

    static final ZoneId BRUXELLES = ZoneId.of("Europe/Brussels");
    static final Clock HORLOGE = Clock.fixed(Instant.parse("2027-03-25T10:00:00Z"), ZoneOffset.UTC);

    @TestBean Clock offeringsClock;
    // bookings ne fournit pas encore de BookedPeriodsProvider : un mock le remplace (aucune réservation par défaut)
    @MockitoBean BookedPeriodsProvider bookedPeriods;
    @Autowired AvailabilityApi availabilityApi;
    @Autowired JdbcClient jdbc;

    static Clock offeringsClock() {
        return HORLOGE;
    }

    // ── Préavis ─────────────────────────────────────────────────────────────

    @Test
    void preavis_exclutLesCreneauxTropProches() {
        long creator = insertCreator("preavis@availability.test", "VALIDATED", false);
        setSettings(creator, 24, 0);
        long tropTot = insertSlot(creator, "2027-03-26T09:00", "2027-03-26T10:00"); // 8 h UTC < 25 mars 10 h + 24 h
        long assezTard = insertSlot(creator, "2027-03-26T14:00", "2027-03-26T15:00");

        assertThat(ids(availabilityApi.findAvailableSlots(creator, LocalDate.of(2027, 3, 26)))).containsExactly(assezTard);
        assertThat(availabilityApi.isBookable(tropTot, insertOffering(creator, 60))).isFalse();
    }

    // ── Marge entre deux rendez-vous ────────────────────────────────────────

    @Test
    void marge_exclutLesCreneauxQuiTouchentUneReservationElargie() {
        long creator = insertCreator("marge@availability.test", "VALIDATED", false);
        setSettings(creator, 0, 60);
        long avantJuste = insertSlot(creator, "2027-04-01T08:30", "2027-04-01T09:00");  // finit pile 1 h avant
        long avantTrop = insertSlot(creator, "2027-04-01T09:15", "2027-04-01T09:45");
        long apresTrop = insertSlot(creator, "2027-04-01T12:30", "2027-04-01T13:30");
        long apresJuste = insertSlot(creator, "2027-04-01T13:00", "2027-04-01T14:00"); // commence pile 1 h après
        when(bookedPeriods.findBookedPeriods(anyLong(), any(), any()))
                .thenReturn(List.of(new BookedPeriod(bruxelles("2027-04-01T10:00"), bruxelles("2027-04-01T12:00"))));

        assertThat(ids(availabilityApi.findAvailableSlots(creator, LocalDate.of(2027, 4, 1))))
                .containsExactly(avantJuste, apresJuste)
                .doesNotContain(avantTrop, apresTrop);
    }

    // ── Durée de la prestation et créateur ──────────────────────────────────

    @Test
    void isBookable_verifieLaDureeEtLeCreateurDeLaPrestation() {
        long creator = insertCreator("duree@availability.test", "VALIDATED", false);
        long autre = insertCreator("autre@availability.test", "VALIDATED", false);
        long uneHeure = insertSlot(creator, "2027-04-02T14:00", "2027-04-02T15:00");

        assertThat(availabilityApi.isBookable(uneHeure, insertOffering(creator, 60))).isTrue();
        assertThat(availabilityApi.isBookable(uneHeure, insertOffering(creator, 90))).isFalse();
        assertThat(availabilityApi.isBookable(uneHeure, insertOffering(autre, 30))).isFalse();
        assertThat(availabilityApi.isBookable(999_999, insertOffering(creator, 30))).isFalse();
    }

    @Test
    void createurNonValideOuSuspendu_aucunCreneau() {
        long enAttente = insertCreator("attente@availability.test", "PENDING", false);
        long suspendu = insertCreator("suspendu@availability.test", "VALIDATED", true);
        long slotAttente = insertSlot(enAttente, "2027-04-02T14:00", "2027-04-02T15:00");
        long slotSuspendu = insertSlot(suspendu, "2027-04-02T14:00", "2027-04-02T15:00");

        assertThat(availabilityApi.findAvailableSlots(enAttente, LocalDate.of(2027, 4, 2))).isEmpty();
        assertThat(availabilityApi.findAvailableSlots(suspendu, LocalDate.of(2027, 4, 2))).isEmpty();
        assertThat(availabilityApi.isBookable(slotAttente, insertOffering(enAttente, 30))).isFalse();
        assertThat(availabilityApi.isBookable(slotSuspendu, insertOffering(suspendu, 30))).isFalse();
        assertThat(availabilityApi.findSlot(slotAttente)).isPresent();
    }

    // ── Fuseau Europe/Brussels ──────────────────────────────────────────────

    @Test
    void fuseau_unCreneauDe23h30ResteSurSonJourABruxelles() {
        long creator = insertCreator("soir@availability.test", "VALIDATED", false);
        long soir = insertSlot(creator, "2027-03-26T23:30", "2027-03-26T23:59"); // 22 h 30 UTC

        assertThat(ids(availabilityApi.findAvailableSlots(creator, LocalDate.of(2027, 3, 26)))).containsExactly(soir);
        assertThat(availabilityApi.findAvailableSlots(creator, LocalDate.of(2027, 3, 27))).isEmpty();
    }

    @Test
    void fuseau_passageAlHeureDete_le28Mars2027() {
        long creator = insertCreator("ete@availability.test", "VALIDATED", false);
        long avant = insertSlot(creator, "2027-03-28T01:00", "2027-03-28T01:45"); // UTC+1
        long apres = insertSlot(creator, "2027-03-28T03:30", "2027-03-28T04:30"); // UTC+2

        var slots = availabilityApi.findAvailableSlots(creator, LocalDate.of(2027, 3, 28));

        assertThat(ids(slots)).containsExactly(avant, apres);
        assertThat(slots.get(0).start().getOffset()).isEqualTo(ZoneOffset.ofHours(1));
        assertThat(slots.get(1).start().getOffset()).isEqualTo(ZoneOffset.ofHours(2));
        assertThat(slots.get(1).start().toLocalDateTime()).isEqualTo(LocalDateTime.parse("2027-03-28T03:30"));
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    private static List<Long> ids(List<AvailableSlot> slots) {
        return slots.stream().map(AvailableSlot::id).toList();
    }

    private static OffsetDateTime bruxelles(String heureLocale) {
        return LocalDateTime.parse(heureLocale).atZone(BRUXELLES).toOffsetDateTime();
    }

    private long insertSlot(long creatorId, String debut, String fin) {
        return jdbc.sql("INSERT INTO availability_slots (creator_id, starts_at, ends_at) VALUES (:c, :d, :f) RETURNING id")
                .param("c", creatorId).param("d", bruxelles(debut)).param("f", bruxelles(fin))
                .query(Long.class).single();
    }

    private void setSettings(long creatorId, int preavisHeures, int margeMinutes) {
        jdbc.sql("INSERT INTO scheduling_settings (creator_id, min_notice_hours, buffer_minutes) VALUES (:c, :p, :m)")
                .param("c", creatorId).param("p", preavisHeures).param("m", margeMinutes)
                .update();
    }

    private long insertOffering(long creatorId, int dureeMinutes) {
        return jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                VALUES (:c, 'Prestation', :d, 100) RETURNING id
                """)
                .param("c", creatorId).param("d", dureeMinutes)
                .query(Long.class).single();
    }

    private long insertCreator(String email, String status, boolean suspended) {
        long id = jdbc.sql("INSERT INTO users (email, sub, role, suspended) VALUES (:e, :s, 'CREATOR', :susp) RETURNING id")
                .param("e", email).param("s", "sub-" + email).param("susp", suspended)
                .query(Long.class).single();
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name, validation_status) VALUES (:id, :n, :st)")
                .param("id", id).param("n", email).param("st", status)
                .update();
        return id;
    }
}
