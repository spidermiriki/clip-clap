package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfilePage;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
class CreatorProfileRepository {

    private final JdbcClient jdbc;

    CreatorProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    CreatorProfile save(Long userId, CreateProfileCommand command) {
        return jdbc.sql("""
                INSERT INTO creator_profiles (user_id, display_name, bio, city, country, service_areas)
                VALUES (:userId, :displayName, :bio, :city, :country, :serviceAreas)
                RETURNING *
                """)
                .param("userId", userId)
                .param("displayName", command.displayName())
                .param("bio", command.bio())
                .param("city", command.city())
                .param("country", command.country())
                .param("serviceAreas", command.serviceAreas())
                .query(CreatorProfileRepository::mapRow)
                .single();
    }

    CreatorProfile update(Long userId, CreateProfileCommand command) {
        return jdbc.sql("""
                UPDATE creator_profiles
                SET display_name = :displayName, bio = :bio, city = :city,
                    country = :country, service_areas = :serviceAreas
                WHERE user_id = :userId
                RETURNING *
                """)
                .param("userId", userId)
                .param("displayName", command.displayName())
                .param("bio", command.bio())
                .param("city", command.city())
                .param("country", command.country())
                .param("serviceAreas", command.serviceAreas())
                .query(CreatorProfileRepository::mapRow)
                .single();
    }

    Set<Long> findAllValidatedUserIds() {
        return new HashSet<>(jdbc.sql("SELECT user_id FROM creator_profiles WHERE validation_status = 'VALIDATED'")
                .query(Long.class)
                .list());
    }

    CreatorProfilePage findAllValidated(int page, int size, Set<Long> excludedUserIds) {
        // NOT IN () est invalide en SQL : la clause n'est ajoutée que s'il y a des ids à exclure
        boolean exclusion = !excludedUserIds.isEmpty();
        String where = "WHERE validation_status = 'VALIDATED'" + (exclusion ? " AND user_id NOT IN (:excluded)" : "");

        var count = jdbc.sql("SELECT COUNT(*) FROM creator_profiles " + where);
        var select = jdbc.sql("SELECT * FROM creator_profiles " + where
                        + " ORDER BY display_name, user_id LIMIT :size OFFSET :offset")
                .param("size", size)
                .param("offset", (long) page * size);
        if (exclusion) {
            count = count.param("excluded", excludedUserIds);
            select = select.param("excluded", excludedUserIds);
        }

        long total = count.query(Long.class).single();
        List<CreatorProfile> items = select.query(CreatorProfileRepository::mapRow).list();
        return new CreatorProfilePage(items, page, size, total);
    }

    Optional<CreatorProfile> findByUserId(Long userId) {
        return jdbc.sql("SELECT * FROM creator_profiles WHERE user_id = :userId")
                .param("userId", userId)
                .query(CreatorProfileRepository::mapRow)
                .optional();
    }

    private static CreatorProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CreatorProfile(
                rs.getLong("user_id"),
                rs.getString("display_name"),
                rs.getString("photo_url"),
                rs.getString("bio"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("service_areas"),
                rs.getString("validation_status")
        );
    }
}
