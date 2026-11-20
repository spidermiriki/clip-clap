package be.heh.clipclap.creators.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/** Types de prestation choisis par un créateur (#87), dans {@code creator_categories}. */
@Repository
class CreatorCategoryRepository {

    private final JdbcClient jdbc;

    CreatorCategoryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<Integer> findCategoryIds(long creatorId) {
        return jdbc.sql("SELECT category_id FROM creator_categories WHERE creator_id = :creatorId ORDER BY category_id")
                .param("creatorId", creatorId)
                .query(Integer.class)
                .list();
    }

    /**
     * Remplace la sélection. Une catégorie inconnue viole la clé étrangère vers {@code categories} :
     * creators ne lit pas la table du module catalog, c'est la base qui la refuse. Transactionnel
     * par lui-même : la suppression et les insertions réussissent ou échouent ensemble.
     */
    @Transactional
    void replace(long creatorId, Collection<Integer> categoryIds) {
        jdbc.sql("DELETE FROM creator_categories WHERE creator_id = :creatorId")
                .param("creatorId", creatorId)
                .update();
        for (Integer categoryId : categoryIds) {
            jdbc.sql("INSERT INTO creator_categories (creator_id, category_id) VALUES (:creatorId, :categoryId)")
                    .param("creatorId", creatorId)
                    .param("categoryId", categoryId)
                    .update();
        }
    }
}
