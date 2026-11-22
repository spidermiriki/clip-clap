package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingStatusChange;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
class BookingRepository {

    private final JdbcClient jdbc;

    BookingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Booking create(long clientId, long creatorId) {
        return jdbc.sql("""
                INSERT INTO bookings (client_id, creator_id)
                VALUES (:clientId, :creatorId)
                RETURNING *
                """)
                .param("clientId", clientId)
                .param("creatorId", creatorId)
                .query(BookingRepository::mapBooking)
                .single();
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
