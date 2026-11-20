package be.heh.clipclap.users;

import java.util.Optional;
import java.util.Set;

/**
 * API publique du module users, utilisable par les autres modules.
 */
public interface UserApi {

	/**
	 * Retrouve l'identifiant interne d'un utilisateur à partir de son identifiant Zitadel.
	 *
	 * @param sub claim {@code sub} du token Zitadel
	 * @return l'id de la table {@code users}, ou vide si aucun compte ne correspond
	 */
	Optional<Long> findIdBySub(String sub);

	/**
	 * Parmi ces identifiants d'utilisateurs, ceux dont le compte est suspendu (#75) : un
	 * créateur suspendu ne doit pas apparaître dans la galerie publique (module {@code catalog}).
	 *
	 * @param userIds identifiants à vérifier (table {@code users})
	 * @return le sous-ensemble de {@code userIds} correspondant à des comptes suspendus
	 */
	Set<Long> findSuspendedIds(Set<Long> userIds);

	/**
	 * Tous les identifiants de comptes suspendus (#98) : permet à {@code catalog} d'exclure
	 * les créateurs suspendus sans devoir d'abord charger tous les créateurs validés.
	 *
	 * @return l'ensemble des ids de la table {@code users} où {@code suspended = true}
	 */
	Set<Long> findAllSuspendedIds();

}
