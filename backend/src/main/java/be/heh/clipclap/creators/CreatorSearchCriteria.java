package be.heh.clipclap.creators;

/**
 * Filtres de la page des profils validés, combinés entre eux (ET logique).
 * Un champ {@code null} ou vide n'est pas appliqué.
 *
 * @param city       ville, sans tenir compte de la casse ni des espaces autour
 * @param country    pays, sans tenir compte de la casse ni des espaces autour
 * @param categoryId identifiant d'un type de prestation (table {@code categories})
 * @param mediaType  {@code PHOTO} ou {@code VIDEO} : créateurs ayant au moins un média de ce type (#15)
 */
public record CreatorSearchCriteria(String city, String country, Integer categoryId, String mediaType) {

    public static final CreatorSearchCriteria AUCUN = new CreatorSearchCriteria(null, null, null, null);
}
