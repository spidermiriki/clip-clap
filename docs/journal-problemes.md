# Journal des problèmes

**Projet** : Clic & Clap — Derouck Arredondo Francisco Homero · Dassi Kouam Gedeon
**Cours** : Développement web avancé 2025-26

Ce journal recense les problèmes rencontrés pendant le projet, leur analyse et la solution retenue. Il sert de base à la section « analyse des problèmes rencontrés et des solutions apportées » du rapport final.

Chaque entrée suit le même plan : **description**, **cause**, **solution**, **règle retenue**.

| # | Problème | Domaine | Date |
|---|----------|---------|------|
| 1 | Cycle de dépendance `creators` ↔ `offerings` | Architecture | Septembre 2026 |
| 2 | Disque plein : image Docker PostgreSQL corrompue | Environnement | 2026-10-09 |
| 3 | Import IcePanel bloqué | Documentation | Octobre 2026 |
| 4 | Convention `main` / `develop` au lieu de GitHub Flow | Git | 2026-10-12 |
| 5 | Documentation déjà mergée écrasée par une PR | Git | 2026-10-12 |
| 6 | Fournisseur d'identité : Zitadel imposé | Sécurité | Octobre 2026 |
| 7 | Version de Java et nom du package racine | Build | 2026-10-12 |
| 8 | `main` cassé par une PR mergée sans CI | CI / tests | 2026-10-17 |
| 9 | Dépôt dans OneDrive : fichiers verrouillés | Environnement | Octobre 2026 |
| 10 | Ports 8080 et 5173 déjà occupés en local | Environnement | 2026-10-16 |
| 11 | Endpoint Zitadel d'attribution de rôle déprécié | Sécurité | 2026-10-29 |
| 12 | Rôle et e-mail absents du profil OIDC côté React | Sécurité | 2026-11-09 |

---

## 1. Cycle de dépendance `creators` ↔ `offerings`

**Description.** Dans la première conception, `creators` dépendait de `offerings` (pour lister les types de prestation d'un créateur) et `offerings` dépendait de `creators` (pour vérifier qu'un créateur est validé). Spring Modulith interdit les cycles : `ApplicationModules.verify()` fait échouer le test de structure.

**Cause.** Deux préoccupations étaient mélangées : le profil du créateur d'un côté, l'assemblage des données publiques de la galerie de l'autre.

**Solution.** Création d'un module `catalog`, en lecture seule, qui agrège les données publiques de `creators`, `offerings` et `reputation`. Aucun module ne dépend de `catalog`, le cycle disparaît (voir [architecture-c4.md](architecture/architecture-c4.md)).

**Règle retenue.** Une vue qui agrège plusieurs modules devient un module de lecture à part, dont personne ne dépend.

---

## 2. Disque plein : image Docker PostgreSQL corrompue

**Description.** Le premier lancement des tests a échoué : le conteneur PostgreSQL s'arrêtait immédiatement avec `exec format error`. Le commit du squelette a lui aussi échoué.

**Cause.** Le disque `C:` était plein (0 Go libre). L'image `postgres:17` avait été téléchargée pendant ce manque de place : son script de démarrage faisait 0 octet. Même après suppression et nouveau téléchargement, Docker réutilisait la couche corrompue de son cache.

**Solution.** Libération d'espace disque, puis passage à l'image `postgres:17-alpine`, dont les couches sont différentes (même version majeure de PostgreSQL).

**Règle retenue.** Vérifier l'espace disque avant un build ou un téléchargement d'images, et lire le journal du conteneur (`docker logs`) avant de chercher dans le code.

---

## 3. Import IcePanel bloqué

**Description.** L'import d'un diagramme dans IcePanel par fichier JSON produisait une architecture incomplète ou une erreur de validation.

**Cause.** L'import d'IcePanel est prévu pour ses propres exports ; son format interne n'est pas documenté et ne tolère pas les modifications manuelles.

**Solution.** Abandon de l'import automatique : les diagrammes sont saisis à la main dans IcePanel en suivant `architecture-c4.md`. Les captures PNG de `docs/architecture/` servent de référence permanente, indépendamment de la période d'essai d'IcePanel.

**Règle retenue.** Garder une copie exportée (PNG) de tout diagramme réalisé dans un outil externe.

---

## 4. Convention `main` / `develop` au lieu de GitHub Flow

**Description.** Le dépôt avait été organisé avec une branche `develop` d'intégration et des branches `feature/<nom>`. La consigne impose le workflow **GitHub Flow**, sans branche `develop`. Une PR avait été mergée dans `main` et une autre dans `develop`, ce qui désynchronisait les deux branches.

**Cause.** Convention reprise d'un autre workflow (Git Flow) avant de relire la consigne.

**Solution.** Passage à GitHub Flow (PR #20) : `main` seule branche permanente, une branche courte par issue nommée `<type>/<n°>-<description>`, PR vers `main` avec `Closes #<n°>`. Suppression de `develop`. Documentation mise à jour (README, plan de release).

**Règle retenue.** Relire la consigne avant de fixer une convention d'équipe, et la documenter dans le README.

---

## 5. Documentation déjà mergée écrasée par une PR

**Description.** Après le merge de la PR #21 (architecture C4), plusieurs changements validés avant elle avaient disparu de `main` : la répartition du travail par module, la section GitHub Flow et les wireframes en images.

**Cause.** La branche de la PR #21 était partie d'une version ancienne de la documentation. Lors du merge, les conflits ont été résolus en gardant uniquement cette version.

**Solution.** PR de restauration (#23) qui combine les deux versions : les changements perdus sont remis, les ajouts de la PR #21 sont conservés.

**Règle retenue.** Toujours partir d'un `main` à jour (`git switch main` puis `git pull`) avant de créer une branche. En cas de conflit, garder le travail des deux versions. Le relecteur vérifie l'onglet « Files changed » : des suppressions dans un fichier que la PR n'est pas censée toucher sont un signal d'alerte.

---

## 6. Fournisseur d'identité : Zitadel imposé

**Description.** La conception initiale prévoyait une connexion par e-mail et mot de passe gérée par notre application, et Google OAuth2 en bonus. La consigne impose **Zitadel**.

**Cause.** Évolution de la consigne (version 2) après la rédaction des premières spécifications.

**Solution.**
- Frontend : flux OIDC avec PKCE ; l'utilisateur est redirigé vers Zitadel pour se connecter ou s'inscrire. Le choix client / créateur se fait dans notre application, avant la redirection.
- Backend : OAuth2 Resource Server. Spring Security vérifie la signature du token d'accès (clés publiques de Zitadel), puis le backend utilise son identifiant `sub` pour retrouver l'utilisateur.
- Aucun mot de passe n'est stocké chez nous : la colonne `users.password_hash` sera remplacée par l'identifiant Zitadel (issue #10). Wireframes de connexion et d'inscription mis à jour (issue #67).

**Règle retenue.** Ne jamais stocker de secret d'authentification quand un fournisseur d'identité peut s'en charger.

---

## 7. Version de Java et nom du package racine

**Description.** Le code utilisait Java 17 et le package `com.clicclap`, alors que la documentation d'architecture indiquait Temurin 25 et le package `be.heh.clicetclap`. La consigne demande des schémas cohérents avec le code réel.

**Cause.** Le code et la documentation avaient été écrits en parallèle, sans décision commune.

**Solution.** Décision du binôme (issue #24) : package `be.heh.clipclap` (domaine inversé de l'école) et Java 17, la version installée sur nos machines, prise en charge par Spring Boot 4 et par les images Docker utilisées. Renommage du package dans tout le code avec `git mv` (historique conservé), puis alignement de la documentation (PR #25).

**Règle retenue.** Fixer les choix structurants (package, version de Java) avant d'écrire du code, et les documenter au même endroit.

---

## 8. `main` cassé par une PR mergée sans CI

**Description.** Juste après le merge de la PR #56 (Docker et Render), la CI a échoué sur `main` : le test `ActuatorHealthIT` ne compilait pas.

**Cause.** La PR #56 a été mergée avant que la CI ne tourne dessus : le workflow GitHub Actions (PR #57) n'était pas encore dans `main` quand la branche avait été créée. Le test contenait trois erreurs : mauvais package pour `RestTestClient` (Spring Framework 7 le place dans `org.springframework.test.web.servlet.client`), annotation `@AutoConfigureRestTestClient` manquante, et une méthode d'assertion inexistante.

**Solution.** Correctif dans l'issue #61 (PR #62) : bon import, annotation ajoutée, `.expectStatus().isOk()`. Les 3 tests passent en local et en CI.

**Règle retenue.** Ne merger une PR qu'avec la CI verte, et activer sur `main` la règle « Require status checks to pass » pour que GitHub l'impose.

---

## 9. Dépôt dans OneDrive : fichiers verrouillés

**Description.** Plusieurs opérations échouaient sur des dossiers : suppression de branches Git (« Deletion of directory … failed »), `git worktree prune`, `./gradlew clean` (« Unable to delete directory build »). Un renommage de package a aussi été appliqué à moitié.

**Cause.** Le dépôt se trouve dans un dossier synchronisé par OneDrive. Pendant la synchronisation, OneDrive (et les outils de VS Code qui surveillent les fichiers) verrouillent des fichiers et des dossiers ; Windows empêche alors Git et Gradle de les supprimer.

**Solution.** Mettre la synchronisation OneDrive en pause pendant le travail. Les dossiers vides restants n'ont aucun effet sur le dépôt. Solution durable : cloner le dépôt hors de OneDrive (par exemple `C:\dev\clip-clap`) ; GitHub sert déjà de sauvegarde.

**Règle retenue.** Ne pas placer un dépôt Git ou un projet Gradle / Node dans un dossier synchronisé.

---

## 10. Ports 8080 et 5173 déjà occupés en local

**Description.** En testant les commandes du README, l'API ne répondait pas sur le port 8080 et la page servie sur le port 5173 n'était pas la nôtre.

**Cause.** Le port 8080 était utilisé par une redirection Kubernetes de Docker Desktop (`kubectl`), et le port 5173 par l'application Vite d'un autre projet.

**Solution.** Vite prend automatiquement le port suivant (5174). Le backend peut être lancé sur un autre port avec `./gradlew bootRun --args=--server.port=8081`. Ces cas sont documentés dans la section « Problèmes fréquents » du README.

**Règle retenue.** Lire l'adresse affichée au démarrage plutôt que de supposer le port, et documenter les problèmes d'environnement rencontrés.

---

## 11. Endpoint Zitadel d'attribution de rôle déprécié

**Description.** La doc Zitadel propose deux endpoints pour attribuer un rôle de projet à un utilisateur : l'API de gestion v1 (`POST /management/v1/users/{userId}/grants`) et l'API v2 (`AuthorizationService.CreateAuthorization`). La référence officielle marque le v1 comme déprécié.

**Cause.** Évolution de l'API Zitadel : le v2 (ressources « Core ») remplace progressivement les endpoints v1 équivalents, sans que l'ancien ne soit retiré immédiatement.

**Solution.** Choix du v2 : `POST /zitadel.authorization.v2.AuthorizationService/CreateAuthorization`, avec le header `Connect-Protocol-Version: 1` et un corps `{ userId, projectId, organizationId, roleKeys }` (voir [`ZitadelRoleClient`](../backend/src/main/java/be/heh/clipclap/users/internal/ZitadelRoleClient.java), issue #11). Contrairement au v1, le v2 exige explicitement l'identifiant de l'organisation : ajout de la variable `ZITADEL_ORG_ID` en plus de `ZITADEL_SERVICE_TOKEN` et `ZITADEL_PROJECT_ID`.

**Règle retenue.** Vérifier la référence officielle d'un endpoint Zitadel avant de l'utiliser (section « deprecated ») plutôt que de se fier à un exemple trouvé ailleurs ; une évolution v1 → v2 change souvent les champs requis du corps de la requête.

---

## 12. Rôle et e-mail absents du profil OIDC côté React

**Description.** En testant la protection des routes (issue #16), un compte de test ayant pourtant le rôle `CREATOR` était systématiquement traité comme n'ayant aucun rôle : `ProtectedRoute` renvoyait toujours vers la galerie, même avec le bon compte. L'e-mail affiché dans la navigation était lui aussi absent.

**Cause.** `react-oidc-context` (via `oidc-client-ts`) ne décode par défaut que les claims minimales présentes dans l'ID token (`iss`, `sub`, `aud`, `exp`, `iat`, `sid`). Les claims étendues demandées par les scopes `profile`, `email` et `urn:zitadel:iam:org:projects:roles` ne sont disponibles qu'auprès de l'endpoint `userinfo` de Zitadel, que la librairie n'interroge que si l'option `loadUserInfo` est activée — elle vaut `false` par défaut et n'avait jamais été configurée.

**Solution.** Ajout de `loadUserInfo: true` dans `frontend/src/auth/oidcConfig.js` (issue #16) : après la connexion, `oidc-client-ts` appelle l'endpoint `userinfo` et fusionne le résultat dans `user.profile`, qui contient alors bien l'e-mail et le claim de rôles.

**Règle retenue.** Un scope accordé (visible dans `user.scope`) ne garantit pas que ses claims soient lisibles dans `user.profile` : avec `oidc-client-ts`, vérifier aussi `loadUserInfo`. Écrire le test du scénario « rôle correct » avant le premier essai manuel dans le navigateur aurait révélé le problème plus tôt.

---

## Mises à jour du journal

| Date | Modification | Auteur |
|------|--------------|--------|
| 2026-10-12 | Création : problèmes 1, 3, 6 et 7 (première version) | Derouck Arredondo Francisco Homero |
| 2026-10-19 | Ajout des problèmes 2, 4, 5, 8, 9 et 10 ; corrections des problèmes 6 et 7 ; plan commun pour chaque entrée | Dassi Kouam Gedeon |
| 2026-10-29 | Ajout du problème 11 (endpoint Zitadel déprécié, issue #11) | Dassi Kouam Gedeon |
| 2026-11-09 | Ajout du problème 12 (`loadUserInfo`, issue #16) | Dassi Kouam Gedeon |
