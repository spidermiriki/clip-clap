# Clic & Clap

Plateforme web de mise en relation entre créateurs visuels (photographes et vidéastes) et clients souhaitant réserver une prestation.

## Binôme

| Nom | Modules dont il est responsable |
|-----|--------------------------------|
| Derouck Arredondo Francisco Homero | `creators`, `offerings`, `catalog`, `admin`, `notifications` (profil, albums, prestations, agenda, galerie, back-office) |
| Dassi Kouam Gedeon | `users`, `bookings`, `reputation`, `moderation` (authentification, réservations, avis, badges, signalements) |

> Chaque module a un seul responsable ; l'autre membre relit ses pull requests. Détail dans le [plan de release](docs/plan-de-release.md#répartition-des-responsabilités).

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Architecture | Monolithe modulaire (Spring Modulith) |
| Backend | Spring Boot 4, contrôleurs REST |
| Authentification | Zitadel (OIDC) + Spring Security |
| Persistance | Spring JDBC (`JdbcClient`) + PostgreSQL |
| Frontend | React (Vite) |
| Stockage médias | Cloudinary |
| Tests | JUnit, Mockito, RestTestClient, Spring Modulith, Testcontainers |
| Conteneurisation | Docker (application + base de données) |
| Build | Gradle |
| Java | 17+ |
| Diagrammes d'architecture | IcePanel (modèle C4) |

## Rôles utilisateurs

- **Visiteur** — navigation publique, galerie filtrée, profils créateurs, inscription
- **Créateur** — gestion du profil, albums et photos, prestations à prix fixe, agenda, traitement des réservations reçues
- **Client** — réservation de prestations (simple ou en pack), suivi et annulation, avis et notes, signalements
- **Administrateur** — validation des comptes créateurs, modération des signalements, gestion des catégories, suspension des comptes

> Les fonctionnalités bonus (favoris, vidéos par lien, recherche avancée) ne seront développées que si le temps le permet. Voir le [plan de release](docs/plan-de-release.md).

## Documentation

- [Fonctionnalités détaillées](docs/fonctionnalites.md)
- [Plan de release](docs/plan-de-release.md)
- [Architecture C4](docs/architecture/architecture-c4.md)
- [Schéma de la base de données](docs/architecture/schema-bd.md)
- [Wireframes](docs/wireframes.md)
- [Plan de tests](docs/tests.md)
- [Journal des problèmes rencontrés](docs/journal-problemes.md)

## Architecture

Clic & Clap est un **monolithe modulaire** : un seul `.jar` Spring Boot, divisé en 9 modules métier (`users`, `creators`, `offerings`, `bookings`, `reputation`, `moderation`, `catalog`, `admin`, `notifications`).

Conventions clés :
- Les données sont accédées via **`JdbcClient`** (pas de Spring Data JPA).
- Chaque module expose son API à la racine du package ; tout le reste est dans `internal` et reste invisible pour les autres modules.
- Les modules communiquent **par événements** (`publishEvent` / `@ApplicationModuleListener`) sauf quand une réponse immédiate est nécessaire (appel direct de l'API publique).
- La structure est vérifiée automatiquement par `ApplicationModules.of(ClipclapApplication.class).verify()` (test `ModularityTests`).

Diagrammes C4 complets : [docs/architecture/architecture-c4.md](docs/architecture/architecture-c4.md)

## Installation et lancement

### Prérequis

| Outil | Version | Utilisation |
|-------|---------|-------------|
| Java (JDK) | 17 | Backend Spring Boot |
| Node.js | 22 ou plus récent | Frontend React (Vite) |
| Docker Desktop | lancé avant de démarrer le backend ou les tests | Base PostgreSQL locale et tests |
| Git | — | Récupération du code |

Gradle n'est pas à installer : le wrapper `gradlew` du dépôt le télécharge automatiquement.

### Récupérer le projet

```bash
git clone https://github.com/spidermiriki/clip-clap.git
cd clip-clap
```

### Lancer le backend

```bash
cd backend
./gradlew bootRun        # Windows (PowerShell) : .\gradlew.bat bootRun
```

- Spring Boot démarre **automatiquement** la base PostgreSQL décrite dans `compose.yaml`, à la racine du dépôt (Docker doit être lancé). Les tables sont créées au démarrage par `schema.sql`.
- L'API répond sur http://localhost:8080.
- La documentation de l'API (Swagger UI) est disponible sur http://localhost:8080/swagger-ui.html.

### Lancer le frontend

Dans un second terminal :

```bash
cd frontend
npm install              # uniquement la première fois, ou après un changement de package.json
npm run dev
```

- L'application s'ouvre sur http://localhost:5173. **Ce port est obligatoire pour la connexion** : c'est la seule adresse de retour autorisée dans Zitadel. Si Vite annonce un autre port, libérez le 5173 ou lancez `npm run dev -- --port 5173 --strictPort`.
- Les appels à `/api` sont redirigés vers le backend sur le port 8080 (`frontend/vite.config.js`).

### Exécuter les tests

```bash
cd backend
./gradlew test           # Windows (PowerShell) : .\gradlew.bat test
```

Docker doit être lancé : les tests démarrent leur propre base PostgreSQL jetable avec Testcontainers, indépendante de la base locale. Le rapport est généré dans `backend/build/reports/tests/test/index.html`.

Pour vérifier le build du frontend :

```bash
cd frontend
npm run build
```

### Configuration

Aucun fichier de configuration n'est nécessaire en local : les valeurs par défaut pointent vers la base de `compose.yaml` et vers l'instance Zitadel du projet.

**Authentification (Zitadel).** La connexion et l'inscription sont déléguées à Zitadel (flux OIDC avec PKCE, aucun mot de passe stocké dans Clic & Clap). Le backend vérifie la signature et l'audience de chaque token ; le compte local est créé à la première connexion. Ces valeurs ne sont pas secrètes et peuvent être remplacées :

| Variable | Utilisée par | Valeur par défaut |
|----------|--------------|-------------------|
| `ZITADEL_ISSUER` | backend | `https://clic-clap-jiufbb.eu1.zitadel.cloud` |
| `ZITADEL_PROJECT_ID` | backend (audience attendue) | ID du projet « Clic & Clap » |
| `VITE_ZITADEL_ISSUER`, `VITE_ZITADEL_CLIENT_ID`, `VITE_ZITADEL_PROJECT_ID` | frontend, dans `frontend/.env.local` (modèle : `frontend/.env.example`) | instance, application `clicclap-web` et projet « Clic & Clap » |

Pour se connecter en local, utiliser un compte de test de l'instance Zitadel (créés dans la console Zitadel, rôles `CLIENT` et `CREATOR`). Les mots de passe ne sont jamais écrits dans le dépôt.

**Attribution des rôles (issue #11).** Le choix client / créateur à l'inscription attribue le rôle dans Zitadel via l'API de gestion, avec un compte de service. Ces valeurs **sont secrètes** et ne doivent jamais être commitées :

| Variable | Utilisée par | Rôle |
|----------|--------------|------|
| `ZITADEL_SERVICE_TOKEN` | backend | Jeton (PAT) d'un compte de service Zitadel avec la permission `user.grant.write` |
| `ZITADEL_ORG_ID` | backend | Identifiant de l'organisation Zitadel (console Zitadel > Organisation > Détails) |

Sans ces deux variables, l'application démarre normalement ; seul `POST /api/users/me/role` échoue tant qu'elles ne sont pas renseignées.

Les identifiants **Cloudinary** (médias) seront ajoutés avec l'upload des photos. Les vrais secrets sont lus depuis des variables d'environnement (fichier `.env`, voir `.env.example`) et **ne doivent jamais être commités**.

### Problèmes fréquents

- **Erreur Docker au démarrage du backend ou des tests** (`docker compose` ou Testcontainers ne trouve pas Docker) : Docker Desktop n'est pas lancé, ou pas encore prêt.
- **Erreur de redirection sur la page Zitadel** (« redirect_uri invalide ») : le frontend ne tourne pas sur le port 5173.
- **`Port 8080 was already in use`** : un autre programme utilise ce port. Arrêtez-le, ou lancez le backend sur un autre port avec `./gradlew bootRun --args=--server.port=8081`. Le proxy `/api` du frontend vise toutefois le port 8080.

## Workflow Git (GitHub Flow)

Le projet suit **GitHub Flow** : `main` est la seule branche permanente et reste toujours déployable. Le suivi des tâches se fait dans le [GitHub Project](https://github.com/users/spidermiriki/projects/6).

1. Chaque tâche est une **issue** du GitHub Project, rattachée au milestone de sa release (`v0.1`, `v0.2`…).
2. Une branche courte part de `main` **à jour** (`git switch main` puis `git pull`) et porte le numéro de l'issue : `<type>/<n°>-<description>`.
3. Les commits suivent les **Conventional Commits** : `feat(users): ...`, `fix(bookings): ...`, `docs: ...`.
4. Une **pull request** vers `main` contient `Closes #<n°>`. Elle est relue et commentée par l'autre membre avant le merge. En cas de conflit, on garde le travail des deux versions.
5. Après le merge, la branche est supprimée.

| Type | Usage | Exemple |
|------|-------|---------|
| `feat` | Nouvelle fonctionnalité | `feat/10-zitadel-auth` |
| `fix` | Correction de bug | `fix/42-collision-creneaux` |
| `test` | Ajout ou correction de tests | `test/7-testcontainers` |
| `docs` | Documentation | `docs/3-github-flow` |
| `ci` | Build, déploiement, GitHub Actions | `ci/6-github-actions` |
| `chore` | Maintenance, configuration | `chore/18-donnees-de-depart` |

Chaque release livrée reçoit un **tag de version** sur `main` (`v0.1`, `v0.2`, `v0.3`, `v1.0`).

## Déploiement

L'application est déployée en continu dès la séance 1 afin d'anticiper les problèmes d'hébergement.

URL de production : *(à compléter après déploiement)*
