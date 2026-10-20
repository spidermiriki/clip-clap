package be.heh.clipclap.users;

import java.util.Optional;

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

}
