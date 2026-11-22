package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfilePage;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Un profil refusé qui est modifié repasse en attente, sans l'ancien motif (#33). */
    CreatorProfile update(Long userId, CreateProfileCommand command) {
        return jdbc.sql("""
                UPDATE creator_profiles
                SET display_name = :displayName, bio = :bio, city = :city,
                    country = :country, service_areas = :serviceAreas,
                    validation_status = CASE WHEN validation_status = 'REJECTED' THEN 'PENDING'
                                             ELSE validation_status END,
                    rejection_reason  = CASE WHEN validation_status = 'REJECTED' THEN NULL
                                             ELSE rejection_reason END
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

    CreatorProfilePage findAllValidated(int page, int size, Set<Long> excludedUserIds,
                                        CreatorSearchCriteria criteria) {
        // Seules les conditions sont assemblées ; les valeurs passent toutes par des paramètres nommés
        List<String> conditions = new ArrayList<>(List.of("validation_status = 'VALIDATED'"));
        Map<String, Object> params = new HashMap<>();
        // NOT IN () est invalide en SQL : la clause n'est ajoutée que s'il y a des ids à exclure
        if (!excludedUserIds.isEmpty()) {
            conditions.add("user_id NOT IN (:excluded)");
            params.put("excluded", excludedUserIds);
        }
        if (StringUtils.hasText(criteria.city())) {
            conditions.add("lower(trim(city)) = lower(trim(:city))");
            params.put("city", criteria.city());
        }
        if (StringUtils.hasText(criteria.country())) {
            conditions.add("lower(trim(country)) = lower(trim(:country))");
            params.put("country", criteria.country());
        }
        if (criteria.categoryId() != null) {
            conditions.add("""
                    EXISTS (SELECT 1 FROM creator_categories cc
                            WHERE cc.creator_id = creator_profiles.user_id AND cc.category_id = :categoryId)""");
            params.put("categoryId", criteria.categoryId());
        }
        // Type de média déjà validé (PHOTO ou VIDEO) par CreatorProfileServiceImpl
        if (StringUtils.hasText(criteria.mediaType())) {
            conditions.add("""
                    EXISTS (SELECT 1 FROM albums a JOIN media m ON m.album_id = a.id
                            WHERE a.creator_id = creator_profiles.user_id AND m.media_type = :mediaType)""");
            params.put("mediaType", criteria.mediaType());
        }
        String where = " WHERE " + String.join(" AND ", conditions);

        long total = jdbc.sql("SELECT COUNT(*) FROM creator_profiles" + where)
                .params(params)
                .query(Long.class)
                .single();
        List<CreatorProfile> items = jdbc.sql("SELECT * FROM creator_profiles" + where
                        + " ORDER BY display_name, user_id LIMIT :size OFFSET :offset")
                .params(params)
                .param("size", size)
                .param("offset", (long) page * size)
                .query(CreatorProfileRepository::mapRow)
                .list();
        return new CreatorProfilePage(items, page, size, total);
    }

    Optional<CreatorProfile> findByUserId(Long userId) {
        return jdbc.sql("SELECT * FROM creator_profiles WHERE user_id = :userId")
                .param("userId", userId)
                .query(CreatorProfileRepository::mapRow)
                .optional();
    }

    List<CreatorProfile> findByValidationStatus(String status) {
        return jdbc.sql("SELECT * FROM creator_profiles WHERE validation_status = :status ORDER BY user_id")
                .param("status", status)
                .query(CreatorProfileRepository::mapRow)
                .list();
    }

    /**
     * Change le statut d'un profil sans condition : utilisé à la création, pour mettre le
     * nouveau profil en attente (#33).
     */
    CreatorProfile setStatus(Long userId, String status) {
        return jdbc.sql("""
                UPDATE creator_profiles SET validation_status = :status, rejection_reason = NULL
                WHERE user_id = :userId
                RETURNING *
                """)
                .param("status", status)
                .param("userId", userId)
                .query(CreatorProfileRepository::mapRow)
                .single();
    }

    /** Vide si le profil n'existe pas ou n'est plus en attente : la condition est dans la requête (#33). */
    Optional<CreatorProfile> decideIfPending(Long userId, String status, String rejectionReason) {
        return jdbc.sql("""
                UPDATE creator_profiles SET validation_status = :status, rejection_reason = :reason
                WHERE user_id = :userId AND validation_status = 'PENDING'
                RETURNING *
                """)
                .param("status", status)
                .param("reason", rejectionReason)
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
                rs.getString("validation_status"),
                rs.getString("rejection_reason")
        );
    }
}
