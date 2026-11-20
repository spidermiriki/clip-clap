package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Choix des types de prestation du créateur connecté (#87), pris en compte par le filtre de la galerie. */
@Service
class CreatorCategoryService {

    // Code SQLSTATE de PostgreSQL pour une clé étrangère violée
    static final String FOREIGN_KEY_VIOLATION = "23503";

    private final CreatorCategoryRepository categories;
    private final CreatorProfileRepository profiles;

    CreatorCategoryService(CreatorCategoryRepository categories, CreatorProfileRepository profiles) {
        this.categories = categories;
        this.profiles = profiles;
    }

    List<Integer> findMine(long userId) {
        requireProfile(userId);
        return categories.findCategoryIds(userId);
    }

    /** Tout ou rien : une catégorie inconnue annule toute la sélection. */
    @Transactional
    List<Integer> replaceMine(long userId, List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("categoryIds doit être une liste d'identifiants");
        }
        requireProfile(userId);
        try {
            categories.replace(userId, new TreeSet<>(categoryIds));
        } catch (DataIntegrityViolationException e) {
            // Seule une clé étrangère violée (23503) signifie « catégorie inconnue » ; toute autre
            // violation (ex. deux PUT simultanés) remonte telle quelle (revue de #122)
            if (e.getMostSpecificCause() instanceof SQLException sql && FOREIGN_KEY_VIOLATION.equals(sql.getSQLState())) {
                throw new UnknownCategoryException();
            }
            throw e;
        }
        return categories.findCategoryIds(userId);
    }

    private void requireProfile(long userId) {
        if (profiles.findByUserId(userId).isEmpty()) {
            throw new CreatorProfileNotFoundException(userId);
        }
    }
}
