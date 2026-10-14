# Journal des problèmes

**Projet** : Clic & Clap — Derouck Arredondo Francisco Homero · Dassi Kouam Gedeon
**Cours** : Développement web avancé — Bac 3 Q1

Ce journal recense les problèmes techniques et décisions d'architecture rencontrés pendant le projet, conformément aux exigences du rapport final.

---

## Problème 1 — Cycle de dépendance `creators ↔ offerings`

**Date** : Septembre 2026 — v1.2 → v1.3

**Description**
Lors de la conception initiale, le module `creators` dépendait de `offerings` (pour lister les types de prestation d'un créateur) et le module `offerings` dépendait de `creators` (pour vérifier qu'un créateur est validé avant de créer une prestation). Ce cycle viole la contrainte de Spring Modulith : `ApplicationModules.verify()` le détecte et fait échouer le test de structure.

**Analyse**
Le problème venait du fait qu'on mélangeait deux préoccupations : (1) le profil du créateur et ses spécialités, (2) l'assemblage des données publiques pour la galerie. La galerie avait besoin à la fois de `creators` et d'`offerings`, ce qui créait une dépendance indirecte circulaire.

**Solution retenue**
Introduction d'un module `catalog` (lecture seule) chargé d'agréger les données publiques depuis `creators`, `offerings`, `reputation` et `bookings`. Ni `creators` ni `offerings` ne dépendent de `catalog` ; aucun autre module ne dépend de `catalog`. Le cycle est rompu. Voir [architecture-c4.md](architecture/architecture-c4.md) v1.3.

**Résultat**
`ApplicationModules.verify()` passe sans erreur ; la galerie et le profil public sont servis par `catalog` sans créer de dépendance inversée.

---

## Problème 2 — Import IcePanel bloqué (fichier JSON)

**Date** : Octobre 2026

**Description**
IcePanel propose d'importer un diagramme existant via un fichier JSON. Après plusieurs tentatives d'export et de ré-import, le fichier JSON généré produisait une architecture incomplète (certains composants étaient perdus) ou déclenchait une erreur de validation côté IcePanel.

**Analyse**
L'interface d'import d'IcePanel est conçue pour ses propres exports — elle ne tolère pas de modifications manuelles du JSON. Le format interne n'est pas documenté et change entre versions.

**Solution retenue**
Abandon de l'import automatique. Les diagrammes sont construits manuellement dans IcePanel en suivant le document `architecture-c4.md` comme référence. Les captures PNG (`docs/architecture/*.png`) servent de référence permanente une fois l'essai IcePanel terminé.

**Résultat**
Les 7 diagrammes C4 + déploiement sont présents dans `docs/architecture/`. L'essai IcePanel reste actif pour les modifications futures.

---

## Problème 3 — Choix ORM : JPA abandonné au profit de `JdbcClient`

**Date** : Septembre 2026

**Description**
Le premier prototype utilisait Spring Data JPA avec Hibernate. Les consignes du cours (chapitre 4 — JdbcClient) interdisent explicitement JPA et imposent `JdbcClient` pour la couche d'accès aux données.

**Analyse**
JPA génère des dépendances entre entités qui rendent difficile le respect de la frontière des modules : les entités JPA peuvent être chargées avec leurs associations, ce qui expose l'interne d'un module à l'autre. `JdbcClient` force à écrire des requêtes SQL explicites et à ne mapper que ce qui est nécessaire, ce qui renforce l'isolation des modules.

**Solution retenue**
Suppression de `spring-boot-starter-data-jpa` et d'Hibernate. Utilisation de `spring-boot-starter-jdbc` + `JdbcClient`. Les repositories sont des classes ordinaires annotées `@Repository` qui délèguent à `JdbcClient`. Le schéma est géré manuellement (ou via Flyway).

**Résultat**
Build plus léger, requêtes SQL explicites et contrôlées, pas de problème de session JPA ou de chargement paresseux entre modules.

---

## Problème 4 — Choix du fournisseur d'identité : Google OAuth2 → Zitadel

**Date** : Octobre 2026 — v0.2

**Description**
La conception initiale mentionnait Google OAuth2 comme fournisseur d'identité (statut « Bonus, à confirmer »). Les consignes v2 du cours imposent **Zitadel** comme fournisseur d'identité obligatoire, avec un flux OIDC/PKCE côté frontend et un OAuth2 Resource Server côté backend.

**Analyse**
Google OAuth2 implique une inscription Google obligatoire pour les utilisateurs, ce qui pose des problèmes pour un projet académique (comptes étudiants, politique de confidentialité). Zitadel est un serveur d'identité auto-hébergeable ou en mode Cloud ; il est imposé par le cours car il permet de configurer des utilisateurs de test sans dépendre d'un compte Google.

**Solution retenue**
- Frontend : flux OIDC/PKCE — le frontend redirige vers Zitadel, récupère un `id_token` (JWT) et l'envoie dans le header `Authorization: Bearer` de chaque requête REST.
- Backend : `spring-boot-starter-oauth2-resource-server` — valide le JWT contre les clés publiques Zitadel (endpoint JWKS). Le module `users` stocke le `sub` (identifiant unique Zitadel) et le rôle ; pas de mot de passe en base.
- Voir [architecture-c4.md](architecture/architecture-c4.md) v1.6 et [fonctionnalites.md](fonctionnalites.md) §1.4.

**Résultat**
Authentification déléguée à Zitadel ; le backend ne gère plus de mots de passe. La table `app_user` contient `sub` (UNIQUE) + `role` + `email` (pour les notifications).

---

## Problème 5 — Version Java : Temurin 25 → Java 17

**Date** : Octobre 2026 — issue #24

**Description**
La documentation initiale prévoyait Temurin 25 (dernière LTS disponible). Le fichier `build.gradle` généré par Spring Initializr avait fixé Java 17 comme version de toolchain. Après discussion au sein du binôme, Java 17 a été retenu.

**Analyse**
Java 25 est disponible mais n'est pas encore une LTS (Long Term Support). Java 17 est la LTS la plus répandue dans les projets Spring Boot actuels et est mieux supportée par les images Docker et les services de CI/CD gratuits (Render, GitHub Actions). Spring Boot 4 requiert Java 17 minimum.

**Solution retenue**
Java 17 maintenu dans `build.gradle` (`toolchain { languageVersion = JavaLanguageVersion.of(17) }`). La documentation a été mise à jour (PR #25).

**Résultat**
Build stable, image Docker `eclipse-temurin:17-jre-alpine` disponible sur Docker Hub, pipeline CI/CD fonctionnel.

---

## Journal des mises à jour

| Date | Entrée | Auteur |
|------|--------|--------|
| 2026-10-12 | Création initiale — 5 problèmes | Francisco Homero |
