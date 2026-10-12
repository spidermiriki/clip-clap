# Clic & Clap

Plateforme web de mise en relation entre créateurs visuels (photographes et vidéastes) et clients souhaitant réserver une prestation.

## Membres du binôme

| Nom | Rôle principal |
|-----|----------------|
| Derouck Arredondo Francisco Homero | Modules `creators`, `offerings`, `catalog`, `admin`, `notifications` (profil, albums, prestations, agenda, galerie, back-office) |
| Dassi Kouam Gedeon | Modules `users`, `bookings`, `reputation`, `moderation` (authentification, réservations, avis, badges, signalements) |

> Chaque module a un seul responsable ; l'autre membre relit ses pull requests. Détail dans le [plan de release](docs/plan-de-release.md#répartition-des-responsabilités).

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Architecture | Monolithe modulaire (Spring Modulith) — voir [docs/architecture/architecture-c4.md](docs/architecture/architecture-c4.md) |
| Backend | Spring Boot 4 |
| Sécurité | Spring Security (rôles) |
| Persistance | Spring JDBC (`JdbcClient`) + PostgreSQL |
| Frontend | React (Vite) |
| Stockage médias | Cloudinary |
| Conteneurisation | Docker (application + base de données) |
| Build | Gradle |
| Java | 17+ |
| Diagrammes d'architecture | IcePanel (modèle C4) |

## Architecture

Clic & Clap est un **monolithe modulaire** : un seul projet Spring Boot, compilé en un seul `.jar` et connecté à une seule base PostgreSQL. Le code est découpé en modules métier (`users`, `creators`, `offerings`, `bookings`, `reputation`, `moderation`, `catalog`, `admin`, `notifications`) qui communiquent uniquement via leur API publique ou par événements.

L'architecture est documentée avec le **modèle C4** (contexte, conteneurs, composants) et un diagramme de déploiement, réalisés dans IcePanel. Le détail se trouve dans [docs/architecture/architecture-c4.md](docs/architecture/architecture-c4.md).

## Rôles utilisateurs

- **Visiteur** — navigation publique, galerie filtrée, profils créateurs, inscription
- **Créateur** — gestion du profil, albums et photos, prestations à prix fixe, agenda, traitement des réservations reçues
- **Client** — réservation de prestations (simple ou en pack), suivi et annulation, avis et notes, signalements
- **Administrateur** — validation des comptes créateurs, modération des signalements, gestion des catégories, suspension des comptes

> Les fonctionnalités bonus (favoris, vidéos par lien, statistiques admin, recherche avancée) ne seront développées que si le temps le permet. Voir le [plan de release](docs/plan-de-release.md).

## Documentation

- [Fonctionnalités détaillées](docs/fonctionnalites.md)
- [Plan de release](docs/plan-de-release.md)
- [Wireframes](docs/wireframes.md)
- [Architecture (monolithe modulaire et modèle C4)](docs/architecture/architecture-c4.md)
- [Plan de tests](docs/tests.md)

## Installation et lancement

> Section à compléter à la fin de la séance 1, une fois le squelette
> de l'application créé et déployé.

Elle contiendra : prérequis, configuration (dont les identifiants Cloudinary,
à ne jamais commiter), commande de lancement en local et commande pour
exécuter les tests.

## Workflow Git (GitHub Flow)

Le projet suit **GitHub Flow** : `main` est la seule branche permanente et reste toujours déployable. Le suivi des tâches se fait dans le [GitHub Project](https://github.com/users/Gedeondk/projects/1).

1. Chaque tâche est une **issue** du GitHub Project, rattachée au milestone de sa release (`v0.1`, `v0.2`…).
2. Une branche courte part de `main` et porte le numéro de l'issue : `<type>/<n°>-<description>`.
3. Les commits suivent les **Conventional Commits** : `feat(users): ...`, `fix(bookings): ...`, `docs: ...`.
4. Une **pull request** vers `main` contient `Closes #<n°>`. Elle est relue et commentée par l'autre membre avant le merge.
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
