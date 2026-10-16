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

## Architecture

Clic & Clap est un **monolithe modulaire** : un seul `.jar` Spring Boot, divisé en 9 modules métier (`users`, `creators`, `offerings`, `bookings`, `reputation`, `moderation`, `catalog`, `admin`, `notifications`).

Conventions clés :
- Les données sont accédées via **`JdbcClient`** (pas de Spring Data JPA).
- Chaque module expose son API à la racine du package ; tout le reste est dans `internal` et reste invisible pour les autres modules.
- Les modules communiquent **par événements** (`publishEvent` / `@ApplicationModuleListener`) sauf quand une réponse immédiate est nécessaire (appel direct de l'API publique).
- La structure est vérifiée automatiquement par `ApplicationModules.of(ClipclapApplication.class).verify()` (test `ModularityTests`).

Diagrammes C4 complets : [docs/architecture/architecture-c4.md](docs/architecture/architecture-c4.md)

## Installation et lancement

> Section à compléter à la fin de la séance 1, une fois le squelette
> de l'application créé et déployé.

Elle contiendra : prérequis, configuration (dont les identifiants Zitadel et Cloudinary, à ne jamais commiter), commande de lancement en local et commande pour exécuter les tests.

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
