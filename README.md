# Clic & Clap

Plateforme web de mise en relation entre créateurs visuels (photographes et vidéastes) et clients souhaitant réserver une prestation.

## Membres du binôme

| Nom | Rôle principal |
|-----|----------------|
| Derouck Arredondo Francisco Homero | Côté créateur (profil, albums, upload, prestations, agenda) |
| Dassi Kouam Gedeon | Côté client (inscription, galerie, réservations, avis, signalements) |

> Les deux membres se croisent sur le back-office administrateur.

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Architecture | Monolithe modulaire (Spring Modulith) — voir [docs/architecture.md](docs/architecture.md) |
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

L'architecture est documentée avec le **modèle C4** (contexte, conteneurs, composants) et un diagramme de déploiement, réalisés dans IcePanel. Le détail se trouve dans [docs/architecture.md](docs/architecture.md).

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
- [Architecture (monolithe modulaire et modèle C4)](docs/architecture.md)
- [Plan de tests](docs/tests.md)

## Installation et lancement

> Section à compléter à la fin de la séance 1, une fois le squelette
> de l'application créé et déployé.

Elle contiendra : prérequis, configuration (dont les identifiants Cloudinary,
à ne jamais commiter), commande de lancement en local et commande pour
exécuter les tests.

## Convention de branches

```
main          — production stable
develop       — intégration continue
feature/<nom> — une branche par fonctionnalité
```

Toute fonctionnalité passe par une **pull request** relue par l'autre membre avant merge.

## Déploiement

L'application est déployée en continu dès la séance 1 afin d'anticiper les problèmes d'hébergement.

URL de production : *(à compléter après déploiement)*
