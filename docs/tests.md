# Plan de tests

Les tests sont réalisés à la fin de chaque release avant le déploiement.
Ils couvrent les scénarios fonctionnels principaux décrits dans [fonctionnalites.md](fonctionnalites.md).

---

## v0.1 — Inscription, profil créateur, galerie

| # | Scénario | Résultat attendu |
|---|----------|------------------|
| 1 | Un visiteur s'inscrit en tant que client | Compte créé, connexion automatique, rôle client actif |
| 2 | Un visiteur s'inscrit en tant que créateur | Compte créé, profil créateur accessible |
| 3 | Un utilisateur se connecte avec de mauvais identifiants | Message d'erreur, pas d'accès |
| 4 | Un créateur remplit son profil (bio, ville, pays, zones) | Données enregistrées et visibles sur le profil public |
| 5 | Un créateur crée un album et uploade deux photos | Album visible avec les deux photos |
| 6 | Un visiteur consulte la galerie sans filtre | Tous les créateurs actifs sont affichés |
| 7 | Un visiteur filtre par type de prestation « Mariage » | Seuls les créateurs proposant ce type apparaissent |
| 8 | Un visiteur filtre par pays | Résultats limités au pays sélectionné |
| 9 | Un visiteur consulte le profil public d'un créateur | Bio, pays, zones, albums et prestations sont affichés |

---

## v0.2 — Prestations, agenda, réservation, validation

| # | Scénario | Résultat attendu |
|---|----------|------------------|
| 10 | Un créateur crée une prestation (nom, durée, prix) | Prestation visible sur son profil public |
| 11 | Un créateur ajoute une option payante à une prestation | Option visible avec son supplément |
| 12 | Un créateur définit des créneaux libres dans son agenda | Créneaux sélectionnables par un client |
| 13 | Un créateur configure un préavis minimum de 48 h | Créneaux dans les 48 h suivantes non proposés au client |
| 14 | Un client réserve un créneau avec une prestation | Réservation créée avec statut « En attente » |
| 15 | Un créateur confirme une réservation | Statut passe à « Confirmée », créneau retiré du calendrier |
| 16 | Un créateur refuse une réservation | Statut passe à « Refusée », créneau libéré |
| 17 | Un client annule une réservation à plus de 48 h | Statut passe à « Annulée », créneau libéré |
| 18 | Un client annule une réservation à moins de 48 h | Statut passe à « Annulée (tardive) », créneau libéré |
| 19 | Une demande n'est pas traitée pendant 7 jours | Statut passe automatiquement à « Expirée », créneau libéré |
| 20 | Un créateur marque une prestation comme terminée | Statut passe à « Terminée », bouton d'avis déverrouillé |
| 21 | Deux clients tentent de réserver le même créneau en même temps | Un seul obtient la réservation, l'autre reçoit un refus |
| 22 | Un créateur tente de réserver ses propres prestations | Action bloquée |
| 23 | Un admin valide un compte créateur en attente | Créateur visible dans la galerie |
| 24 | Un admin refuse un compte créateur | Créateur non activé, n'apparaît pas dans la galerie |

---

## v0.3 — Réservation multiple et avis

| # | Scénario | Résultat attendu |
|---|----------|------------------|
| 25 | Un client réserve 2 créneaux en pack | Total avec 5 % de réduction affiché |
| 26 | Un client réserve 4 créneaux en pack | Total avec 10 % de réduction affiché |
| 27 | Un créneau du pack devient indisponible pendant la validation | Toute la réservation est refusée (atomicité) |
| 28 | Un client laisse un avis après une prestation terminée | Avis et note visibles sur le profil public du créateur |
| 29 | Un client tente de laisser un avis sans prestation terminée | Action bloquée |
| 30 | Un client tente de laisser un deuxième avis sur la même prestation | Action bloquée |
| 31 | Un client tente de laisser un avis plus de 30 jours après la fin | Action bloquée |
| 32 | Un utilisateur signale un avis inapproprié | Signalement enregistré, visible dans le back-office admin |

---

## v1.0 — Back-office, badges et modération

| # | Scénario | Résultat attendu |
|---|----------|------------------|
| 33 | Un admin supprime un contenu signalé | Contenu retiré de la plateforme |
| 34 | Un admin lève un signalement | Signalement retiré, contenu maintenu |
| 35 | Un admin crée un nouveau type de prestation | Catégorie disponible dans les filtres et les formulaires |
| 36 | Un créateur atteint 10 avis avec une note moyenne ≥ 4,5 | Badge « Top créateur » affiché sur son profil |
| 37 | Un créateur répond à toutes ses demandes en moins de 24 h | Badge « Réponse rapide » affiché sur son profil |
| 38 | Un créateur a moins de 3 prestations terminées | Badge « Nouveau » affiché |
| 39 | Un admin suspend un compte utilisateur | Utilisateur ne peut plus se connecter, profil retiré de la galerie |
| 40 | On tente de supprimer une prestation liée à une réservation confirmée | Action bloquée, message d'erreur |

---

## Tests automatisés

Dépendances Gradle à ajouter :
- `testImplementation 'org.springframework.modulith:spring-modulith-starter-test'` — pour `@ApplicationModuleTest` et `Scenario`
- `testImplementation 'org.springframework.boot:spring-boot-testcontainers'` — pour `@ServiceConnection`
- `testImplementation 'org.testcontainers:postgresql'` — image PostgreSQL

### Dès v0.1 — Structure modulaire

| Classe de test | Type | Ce qu'elle vérifie |
|----------------|------|---------------------|
| `ModularityTests` | `ApplicationModules.verify()` | Aucun cycle entre modules, visibilité `internal` respectée |
| `AuthControllerTest` | `@SpringBootTest` + `RestTestClient` | Inscription retourne 201, token Zitadel invalide retourne 401 |
| `GalleryControllerTest` | `@SpringBootTest` + `RestTestClient` | GET `/api/gallery` retourne la liste ; filtre par pays filtre les résultats |
| `JdbcUserRepositoryTest` | `@JdbcTest` + Testcontainers (`postgres:17-alpine`) | `save` puis `findBySub` retourne l'utilisateur ; sub dupliqué lève une exception |

### Dès v0.2 — Événements de réservation

| Classe de test | Type | Ce qu'elle vérifie |
|----------------|------|---------------------|
| `BookingControllerTest` | `@SpringBootTest` + `RestTestClient` | POST `/api/bookings` retourne 201 ; créneau déjà pris retourne 409 |
| `JdbcBookingRepositoryTest` | `@JdbcTest` + Testcontainers | `save` et `findById` ; deux inserts sur le même créneau violent la contrainte d'unicité |
| `BookingEventsTest` | `@ApplicationModuleTest` + `Scenario` / `PublishedEvents` | Confirmer une réservation publie `BookingConfirmed` ; `BookingEventListener` reçoit l'événement |

**Exemple de test d'événement :**

```java
@ApplicationModuleTest
class BookingEventsTest {

    @Test
    void confirmingBookingPublishesEvent(Scenario scenario) {
        scenario.stimulate(bookings -> bookings.confirm(42L))
                .andWaitForEventOfType(BookingConfirmed.class)
                .toArriveAndVerify(event -> assertThat(event.bookingId()).isEqualTo(42L));
    }
}
```

---

## Notes

- Les tests manuels (scénarios 1–40) sont réalisés sur l'environnement déployé en ligne à la fin de chaque release.
- Les tests automatisés couvrent les cas critiques (anti-collision, événements, structure modulaire). Ils sont écrits en même temps que le code, lancés avec `./gradlew test` (dans `backend/`) et exécutés en CI à chaque pull request vers `main`.
- Outils : **JUnit** (tests unitaires), **Mockito** (stubs et mocks), **RestTestClient** (contrôleurs REST), **Spring Modulith** (`ModularityTests` et `@ApplicationModuleTest`), **Testcontainers** (PostgreSQL).
- Les tests qui ont besoin de la base importent `TestcontainersConfiguration` (`@Import(TestcontainersConfiguration.class)`) : chaque lancement démarre une base `postgres:17-alpine` jetable, la même image que `compose.yaml`, indépendante de la base locale. Seul Docker doit être lancé.
