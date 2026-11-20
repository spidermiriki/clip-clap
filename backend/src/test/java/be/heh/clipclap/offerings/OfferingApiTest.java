package be.heh.clipclap.offerings;

import be.heh.clipclap.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** API publique utilisée par catalog (profil public) et bookings (prix d'une réservation). */
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@Transactional
class OfferingApiTest {

    @Autowired OfferingApi offeringApi;
    @Autowired JdbcClient jdbc;

    long creatorId;
    long mariage;
    long drone;
    long album;

    @BeforeEach
    void setUp() {
        creatorId = jdbc.sql("INSERT INTO users (email, sub, role) VALUES ('api@offerings.test', 'sub-api', 'CREATOR') RETURNING id")
                .query(Long.class).single();
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name) VALUES (:id, 'Api')").param("id", creatorId).update();
        mariage = insertOffering("Reportage mariage", "1200.00");
        insertOffering("Portrait", "90.00");
        drone = insertOption(mariage, "Drone", "150.00");
        album = insertOption(mariage, "Album photo", "89.90");
    }

    @Test
    void findOfferingsOf_dansLOrdreDeCreation_avecLeursOptions() {
        var offerings = offeringApi.findOfferingsOf(creatorId);

        assertThat(offerings).extracting(Offering::name).containsExactly("Reportage mariage", "Portrait");
        assertThat(offerings.get(0).options()).extracting(OfferingOption::name).containsExactly("Drone", "Album photo");
        assertThat(offerings.get(1).options()).isEmpty();
    }

    @Test
    void findOffering_inconnue_renvoieVide() {
        assertThat(offeringApi.findOffering(mariage)).map(Offering::creatorUserId).contains(creatorId);
        assertThat(offeringApi.findOffering(999_999)).isEmpty();
    }

    @Test
    void quote_additionneLePrixDeBaseEtLesOptionsChoisies() {
        var quote = offeringApi.quote(mariage, Set.of(drone, album)).orElseThrow();

        assertThat(quote.basePrice()).isEqualByComparingTo("1200");
        assertThat(quote.selectedOptions()).hasSize(2);
        assertThat(quote.total()).isEqualByComparingTo(new BigDecimal("1439.90"));
        assertThat(offeringApi.quote(mariage, Set.of()).orElseThrow().total()).isEqualByComparingTo("1200");
    }

    @Test
    void quote_optionDUneAutrePrestation_refusee() {
        long autre = insertOffering("Clip", "500.00");
        long optionDuClip = insertOption(autre, "Montage", "100.00");

        assertThatThrownBy(() -> offeringApi.quote(mariage, Set.of(drone, optionDuClip)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(offeringApi.quote(999_999, Set.of())).isEmpty();
    }

    private long insertOffering(String name, String price) {
        return jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                VALUES (:creatorId, :name, 60, :price)
                RETURNING id
                """)
                .param("creatorId", creatorId).param("name", name).param("price", new BigDecimal(price))
                .query(Long.class).single();
    }

    private long insertOption(long offeringId, String name, String price) {
        return jdbc.sql("INSERT INTO offering_options (offering_id, name, extra_price) VALUES (:id, :name, :price) RETURNING id")
                .param("id", offeringId).param("name", name).param("price", new BigDecimal(price))
                .query(Long.class).single();
    }
}
