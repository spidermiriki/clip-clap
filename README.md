# Clic & Clap

Plateforme web de mise en relation entre créateurs visuels (photographes et vidéastes) et clients souhaitant réserver une prestation.

## Binôme

| Nom | Rôle principal |
|-----|----------------|
| Derouck Arredondo Francisco Homero | Côté créateur (profil, albums, prestations, agenda) |
| Dassi Kouam Gedeon | Côté client (réservation, avis, signalement) |

> Les deux membres se croisent sur le back-office administrateur.

## Documentation

- [Cahier des charges](docs/cdc-clic-et-clap.md)
- [Fonctionnalités détaillées](docs/fonctionnalites.md)
- [Plan de release](docs/plan-de-release.md)
- [Architecture C4](docs/architecture-c4.md)
- [Wireframes](docs/wireframes.md)
- [Plan de tests](docs/tests.md)

## Architecture

Clic & Clap est un **monolithe modulaire** : un seul `.jar` Spring Boot, divisé en 9 modules métier (`users`, `creators`, `offerings`, `bookings`, `reputation`, `moderation`, `catalog`, `admin`, `notifications`).

Conventions clés :
- Les données sont accédées via **`JdbcClient`** (pas de Spring Data JPA).
- Chaque module expose son API à la racine du package ; tout le reste est dans `internal` et reste invisible pour les autres modules.
- Les modules communiquent **par événements** (`publishEvent` / `@ApplicationModuleListener`) sauf quand une réponse immédiate est nécessaire (appel direct de l'API publique).
- La structure est vérifiée automatiquement par `ApplicationModules.of(ClicEtClapApplication.class).verify()`.

Diagrammes C4 complets : [docs/architecture-c4.md](docs/architecture-c4.md)

## Installation et lancement

> Section à compléter à la fin de la séance 1, une fois le squelette
> de l'application créé et déployé.

Elle contiendra : prérequis, configuration, commande de lancement en local et commande pour exécuter les tests.

## Convention de branches

```
main          — production stable
develop       — intégration continue
feature/<nom> — une branche par fonctionnalité
```

Toute fonctionnalité passe par une **pull request** relue par l'autre membre avant merge.

## Déploiement

L'application est déployée en continu dès la séance 1.

URL de production : *(à compléter après déploiement)*
