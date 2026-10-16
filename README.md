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
- [Wireframes](docs/wireframes.md)
- [Plan de tests](docs/tests.md)

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

- Spring Boot démarre **automatiquement** la base PostgreSQL décrite dans `backend/compose.yaml` (Docker doit être lancé). Les tables sont créées au démarrage par `schema.sql`.
- L'API répond sur http://localhost:8080.
- La documentation de l'API (Swagger UI) est disponible sur http://localhost:8080/swagger-ui.html.

### Lancer le frontend

Dans un second terminal :

```bash
cd frontend
npm install              # uniquement la première fois, ou après un changement de package.json
npm run dev
```

- L'application s'ouvre à l'adresse affichée dans le terminal, par défaut http://localhost:5173 (Vite prend le port suivant si celui-ci est occupé).
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

Aucune configuration n'est nécessaire pour l'instant : les identifiants de la base locale sont fournis par `backend/compose.yaml`.

Les identifiants **Zitadel** (authentification) et **Cloudinary** (médias) seront ajoutés avec les fonctionnalités correspondantes. Ils seront lus depuis des variables d'environnement et **ne doivent jamais être commités**.

### Problèmes fréquents

- **Erreur Docker au démarrage du backend ou des tests** (`docker compose` ou Testcontainers ne trouve pas Docker) : Docker Desktop n'est pas lancé, ou pas encore prêt.
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
