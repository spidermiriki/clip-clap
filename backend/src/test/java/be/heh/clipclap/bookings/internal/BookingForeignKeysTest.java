package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Clés étrangères de bookings vers offerings et availability_slots (#133) : supprimer une prestation
 * ou un créneau ne doit pas échouer à cause d'une réservation passée, qui garde son historique.
 */
@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class BookingForeignKeysTest {

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @Test
    void contraintes_nommeesParPostgres_etEnOnDeleteSetNull() {
        assertThat(reglesDeSuppression()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "bookings_offering_id_fkey", "n",
                "bookings_slot_id_fkey", "n"));
    }

    @Test
    void schemaRejoue_recreeLesContraintesSansDoublon() {
        // Même chose qu'un redémarrage : la table et ses contraintes existent déjà
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);

        assertThat(reglesDeSuppression()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "bookings_offering_id_fkey", "n",
                "bookings_slot_id_fkey", "n"));
    }

    @Test
    void supprimerPrestationEtCreneau_dUneReservationRefusee_laGardeAvecDesReferencesNulles() {
        long client = insertUser("client@fk.test", "sub-client-fk", "CLIENT");
        long creator = insertUser("creator@fk.test", "sub-creator-fk", "CREATOR");
        jdbc.sql("INSERT INTO creator_profiles (user_id, display_name) VALUES (:id, 'Créateur')").param("id", creator).update();
        long offering = jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price)
                VALUES (:creator, 'Reportage', 60, 300) RETURNING id
                """).param("creator", creator).query(Long.class).single();
        long slot = jdbc.sql("""
                INSERT INTO availability_slots (creator_id, starts_at, ends_at)
                VALUES (:creator, '2027-04-02T14:00+02:00', '2027-04-02T15:00+02:00') RETURNING id
                """).param("creator", creator).query(Long.class).single();
        long booking = jdbc.sql("""
                INSERT INTO bookings (client_id, creator_id, offering_id, slot_id, total_price, status)
                VALUES (:client, :creator, :offering, :slot, 300, 'REFUSEE') RETURNING id
                """)
                .param("client", client).param("creator", creator)
                .param("offering", offering).param("slot", slot)
                .query(Long.class).single();

        jdbc.sql("DELETE FROM offerings WHERE id = :id").param("id", offering).update();
        jdbc.sql("DELETE FROM availability_slots WHERE id = :id").param("id", slot).update();

        var restante = jdbc.sql("SELECT offering_id, slot_id, total_price, status FROM bookings WHERE id = :id")
                .param("id", booking)
                .query((rs, rowNum) -> new Object[] {
                        rs.getObject("offering_id"), rs.getObject("slot_id"), rs.getBigDecimal("total_price"), rs.getString("status")})
                .single();
        assertThat(restante[0]).isNull();
        assertThat(restante[1]).isNull();
        assertThat((BigDecimal) restante[2]).isEqualByComparingTo("300");
        assertThat(restante[3]).isEqualTo("REFUSEE");
    }

    /** Clés étrangères de bookings vers offerings et availability_slots : nom → règle (« n » = SET NULL). */
    private Map<String, String> reglesDeSuppression() {
        List<Map.Entry<String, String>> lignes = jdbc.sql("""
                SELECT conname, confdeltype::text AS regle FROM pg_constraint
                WHERE conrelid = 'bookings'::regclass AND contype = 'f'
                  AND confrelid IN ('offerings'::regclass, 'availability_slots'::regclass)
                """)
                .query((rs, rowNum) -> Map.entry(rs.getString("conname"), rs.getString("regle")))
                .list();
        assertThat(lignes).as("une seule contrainte par colonne").hasSize(2);
        return Map.ofEntries(lignes.toArray(Map.Entry[]::new));
    }

    private long insertUser(String email, String sub, String role) {
        return jdbc.sql("INSERT INTO users (email, sub, role) VALUES (:email, :sub, :role) RETURNING id")
                .param("email", email).param("sub", sub).param("role", role)
                .query(Long.class).single();
    }
}
