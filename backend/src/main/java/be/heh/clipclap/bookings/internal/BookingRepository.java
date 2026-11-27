package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingStatusChange;
import be.heh.clipclap.offerings.BookedPeriod;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
class BookingRepository {

    private final JdbcClient jdbc;

    BookingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Booking create(long clientId, long creatorId, long offeringId, long slotId, BigDecimal totalPrice) {
        return jdbc.sql("""
                INSERT INTO bookings (client_id, creator_id, offering_id, slot_id, total_price)
                VALUES (:clientId, :creatorId, :offeringId, :slotId, :totalPrice)
                RETURNING *
                """)
                .param("clientId", clientId)
                .param("creatorId", creatorId)
                .param("offeringId", offeringId)
                .param("slotId", slotId)
                .param("totalPrice", totalPrice)
                .query(BookingRepository::mapBooking)
                .single();
    }

    /** Pour {@link be.heh.clipclap.offerings.OfferingUsageChecker} : prestation liée à une réservation active. */
    boolean existsActiveForOffering(long offeringId) {
        return Boolean.TRUE.equals(jdbc.sql("""
                SELECT EXISTS(
                    SELECT 1 FROM bookings
                    WHERE offering_id = :offeringId AND status IN ('EN_ATTENTE', 'CONFIRMEE')
                )
                """)
                .param("offeringId", offeringId)
                .query(Boolean.class)
                .single());
    }

    /** Pour {@link be.heh.clipclap.offerings.SlotUsageChecker} : créneau lié à une réservation active. */
    boolean existsActiveForSlot(long slotId) {
        return Boolean.TRUE.equals(jdbc.sql("""
                SELECT EXISTS(
                    SELECT 1 FROM bookings
                    WHERE slot_id = :slotId AND status IN ('EN_ATTENTE', 'CONFIRMEE')
                )
                """)
                .param("slotId", slotId)
                .query(Boolean.class)
                .single());
    }

    /**
     * Pour {@link be.heh.clipclap.offerings.BookedPeriodsProvider} : périodes actives (En
     * attente ou Confirmée) qui chevauchent [from, to) — une demande non encore traitée doit
     * aussi faire respecter la marge (#30, revue de #139) : #31 (accepter/refuser) ne referait
     * aucun contrôle de marge au moment de confirmer.
     */
    List<BookedPeriod> findActivePeriods(long creatorId, OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT s.starts_at, s.ends_at
                FROM bookings b
                JOIN availability_slots s ON s.id = b.slot_id
                WHERE b.creator_id = :creatorId AND b.status IN ('EN_ATTENTE', 'CONFIRMEE')
                  AND s.starts_at < :to AND s.ends_at > :from
                """)
                .param("creatorId", creatorId)
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new BookedPeriod(
                        rs.getObject("starts_at", OffsetDateTime.class),
                        rs.getObject("ends_at", OffsetDateTime.class)))
                .list();
    }

    Optional<Booking> findById(long bookingId) {
        return jdbc.sql("SELECT * FROM bookings WHERE id = :bookingId")
                .param("bookingId", bookingId)
                .query(BookingRepository::mapBooking)
                .optional();
    }

    /**
     * Transition conditionnée par le statut lu : la condition {@code AND status = :statutActuel}
     * porte l'exclusion mutuelle jusque dans la requête, sans verrou explicite. Renvoie {@code false}
     * si aucune ligne n'a été modifiée, c'est-à-dire si le statut a déjà changé entre la lecture et
     * cette écriture (requête concurrente).
     */
    boolean updateStatusSiActuel(long bookingId, String statutActuel, String nouveauStatut) {
        int lignesModifiees = jdbc.sql("""
                UPDATE bookings SET status = :nouveauStatut
                WHERE id = :bookingId AND status = :statutActuel
                """)
                .param("nouveauStatut", nouveauStatut)
                .param("bookingId", bookingId)
                .param("statutActuel", statutActuel)
                .update();
        return lignesModifiees == 1;
    }

    void insertHistoryEntry(long bookingId, long actorId, String previousStatus, String newStatus) {
        jdbc.sql("""
                INSERT INTO booking_status_history (booking_id, actor_id, previous_status, new_status)
                VALUES (:bookingId, :actorId, :previousStatus, :newStatus)
                """)
                .param("bookingId", bookingId)
                .param("actorId", actorId)
                .param("previousStatus", previousStatus)
                .param("newStatus", newStatus)
                .update();
    }

    /** Historique du plus ancien au plus récent (ordre chronologique stable). */
    List<BookingStatusChange> findHistory(long bookingId) {
        return jdbc.sql("""
                SELECT actor_id, previous_status, new_status, changed_at
                FROM booking_status_history
                WHERE booking_id = :bookingId
                ORDER BY changed_at, id
                """)
                .param("bookingId", bookingId)
                .query(BookingRepository::mapStatusChange)
                .list();
    }

    private static Booking mapBooking(ResultSet rs, int rowNum) throws SQLException {
        return new Booking(
                rs.getLong("id"),
                rs.getLong("client_id"),
                rs.getLong("creator_id"),
                // offering_id/slot_id sont nullables (ON DELETE SET NULL, #133) : getLong renverrait 0
                // au lieu de null pour une réservation passée dont la prestation ou le créneau a été
                // supprimé depuis.
                rs.getObject("offering_id", Long.class),
                rs.getObject("slot_id", Long.class),
                rs.getBigDecimal("total_price"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private static BookingStatusChange mapStatusChange(ResultSet rs, int rowNum) throws SQLException {
        return new BookingStatusChange(
                rs.getLong("actor_id"),
                rs.getString("previous_status"),
                rs.getString("new_status"),
                toInstant(rs.getTimestamp("changed_at"))
        );
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
