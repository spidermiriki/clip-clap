package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.offerings.AvailableSlot;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Créneaux libres ({@code availability_slots}) et réglages ({@code scheduling_settings}) des créateurs (#28). */
@Repository
class AvailabilityRepository {

    static final ZoneId BRUXELLES = ZoneId.of("Europe/Brussels");

    private final JdbcClient jdbc;

    AvailabilityRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    record Settings(int minNoticeHours, int bufferMinutes) {
        static final Settings AUCUN = new Settings(0, 0);
    }

    // ── Créneaux ────────────────────────────────────────────────────────────

    AvailableSlot create(long creatorId, OffsetDateTime start, OffsetDateTime end) {
        return jdbc.sql("""
                INSERT INTO availability_slots (creator_id, starts_at, ends_at)
                VALUES (:creatorId, :start, :end)
                RETURNING id, creator_id, starts_at, ends_at
                """)
                .param("creatorId", creatorId)
                .param("start", start)
                .param("end", end)
                .query(AvailabilityRepository::mapSlot)
                .single();
    }

    /** Vrai si un autre créneau du créateur chevauche {@code [start, end)} (se toucher n'est pas chevaucher). */
    boolean overlaps(long creatorId, OffsetDateTime start, OffsetDateTime end) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM availability_slots
                               WHERE creator_id = :creatorId AND starts_at < :end AND ends_at > :start)
                """)
                .param("creatorId", creatorId)
                .param("start", start)
                .param("end", end)
                .query(Boolean.class)
                .single();
    }

    /** Créneaux du créateur qui commencent dans {@code [from, to)}, du plus tôt au plus tard. */
    List<AvailableSlot> findStartingBetween(long creatorId, OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT id, creator_id, starts_at, ends_at FROM availability_slots
                WHERE creator_id = :creatorId AND starts_at >= :from AND starts_at < :to
                ORDER BY starts_at, id
                """)
                .param("creatorId", creatorId)
                .param("from", from)
                .param("to", to)
                .query(AvailabilityRepository::mapSlot)
                .list();
    }

    Optional<AvailableSlot> findById(long slotId) {
        return jdbc.sql("SELECT id, creator_id, starts_at, ends_at FROM availability_slots WHERE id = :id")
                .param("id", slotId)
                .query(AvailabilityRepository::mapSlot)
                .optional();
    }

    void delete(long slotId) {
        jdbc.sql("DELETE FROM availability_slots WHERE id = :id").param("id", slotId).update();
    }

    // ── Réglages ────────────────────────────────────────────────────────────

    Settings findSettings(long creatorId) {
        return jdbc.sql("SELECT min_notice_hours, buffer_minutes FROM scheduling_settings WHERE creator_id = :creatorId")
                .param("creatorId", creatorId)
                .query(Settings.class)
                .optional()
                .orElse(Settings.AUCUN);
    }

    Settings saveSettings(long creatorId, Settings settings) {
        return jdbc.sql("""
                INSERT INTO scheduling_settings (creator_id, min_notice_hours, buffer_minutes)
                VALUES (:creatorId, :notice, :buffer)
                ON CONFLICT (creator_id) DO UPDATE
                    SET min_notice_hours = EXCLUDED.min_notice_hours, buffer_minutes = EXCLUDED.buffer_minutes
                RETURNING min_notice_hours, buffer_minutes
                """)
                .param("creatorId", creatorId)
                .param("notice", settings.minNoticeHours())
                .param("buffer", settings.bufferMinutes())
                .query(Settings.class)
                .single();
    }

    /** Les heures sont rendues à l'heure de Bruxelles, quel que soit le fuseau de la session. */
    private static AvailableSlot mapSlot(ResultSet rs, int rowNum) throws SQLException {
        return new AvailableSlot(
                rs.getLong("id"),
                rs.getLong("creator_id"),
                rs.getObject("starts_at", OffsetDateTime.class).atZoneSameInstant(BRUXELLES).toOffsetDateTime(),
                rs.getObject("ends_at", OffsetDateTime.class).atZoneSameInstant(BRUXELLES).toOffsetDateTime());
    }
}
