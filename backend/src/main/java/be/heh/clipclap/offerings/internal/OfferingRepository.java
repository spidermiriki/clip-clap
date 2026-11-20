package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.offerings.Offering;
import be.heh.clipclap.offerings.OfferingOption;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
class OfferingRepository {

    private final JdbcClient jdbc;

    OfferingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private record Row(long id, long creatorId, String name, int durationMinutes, BigDecimal basePrice, Integer categoryId) {}

    long create(long creatorId, String name, int durationMinutes, BigDecimal basePrice, Integer categoryId) {
        return jdbc.sql("""
                INSERT INTO offerings (creator_id, name, duration_minutes, base_price, category_id)
                VALUES (:creatorId, :name, :duration, :price, :categoryId)
                RETURNING id
                """)
                .param("creatorId", creatorId)
                .param("name", name)
                .param("duration", durationMinutes)
                .param("price", basePrice)
                .param("categoryId", categoryId)
                .query(Long.class)
                .single();
    }

    /** Faux si la prestation n'existe pas ou appartient à un autre créateur. */
    boolean update(long offeringId, long creatorId, String name, int durationMinutes, BigDecimal basePrice,
                   Integer categoryId) {
        return jdbc.sql("""
                UPDATE offerings SET name = :name, duration_minutes = :duration, base_price = :price,
                                     category_id = :categoryId
                WHERE id = :id AND creator_id = :creatorId
                """)
                .param("name", name)
                .param("duration", durationMinutes)
                .param("price", basePrice)
                .param("categoryId", categoryId)
                .param("id", offeringId)
                .param("creatorId", creatorId)
                .update() == 1;
    }

    boolean isOwnedBy(long offeringId, long creatorId) {
        return jdbc.sql("SELECT COUNT(*) FROM offerings WHERE id = :id AND creator_id = :creatorId")
                .param("id", offeringId)
                .param("creatorId", creatorId)
                .query(Long.class)
                .single() == 1;
    }

    void delete(long offeringId) {
        jdbc.sql("DELETE FROM offerings WHERE id = :id").param("id", offeringId).update();
    }

    void replaceOptions(long offeringId, List<OfferingCommand.OptionCommand> options) {
        jdbc.sql("DELETE FROM offering_options WHERE offering_id = :id").param("id", offeringId).update();
        for (var option : options) {
            jdbc.sql("INSERT INTO offering_options (offering_id, name, extra_price) VALUES (:id, :name, :price)")
                    .param("id", offeringId)
                    .param("name", option.name())
                    .param("price", option.extraPrice())
                    .update();
        }
    }

    Optional<Offering> findById(long offeringId) {
        return withOptions(jdbc.sql("""
                SELECT id, creator_id, name, duration_minutes, base_price, category_id FROM offerings WHERE id = :id
                """)
                .param("id", offeringId)
                .query(Row.class)
                .list())
                .stream().findFirst();
    }

    /** Prestations du créateur (de la plus ancienne à la plus récente) avec leurs options, en deux requêtes. */
    List<Offering> findByCreator(long creatorId) {
        return withOptions(jdbc.sql("""
                SELECT id, creator_id, name, duration_minutes, base_price, category_id FROM offerings
                WHERE creator_id = :creatorId
                ORDER BY created_at, id
                """)
                .param("creatorId", creatorId)
                .query(Row.class)
                .list());
    }

    private List<Offering> withOptions(Collection<Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<OfferingOption>> options = new LinkedHashMap<>();
        rows.forEach(r -> options.put(r.id(), new ArrayList<>()));
        jdbc.sql("""
                SELECT id, offering_id, name, extra_price FROM offering_options
                WHERE offering_id IN (:ids)
                ORDER BY id
                """)
                .param("ids", options.keySet())
                .query((rs, rowNum) -> options.get(rs.getLong("offering_id"))
                        .add(new OfferingOption(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("extra_price"))))
                .list();
        return rows.stream()
                .map(r -> new Offering(r.id(), r.creatorId(), r.name(), r.durationMinutes(), r.basePrice(), r.categoryId(),
                        List.copyOf(options.get(r.id()))))
                .toList();
    }
}
