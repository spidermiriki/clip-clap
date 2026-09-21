# Clic & Clap

Plateforme web de mise en relation entre créateurs visuels (photographes et vidéastes) et clients souhaitant réserver une prestation.

## Membres du binôme

| Nom | Rôle principal |
|-----|----------------|
| Derouck Arredondo Francisco Homero | Côté créateur (profil, albums, upload) |
| Dassi Kouam Gedeon | Côté client (demandes, avis, favoris) |

> Les deux membres se croisent sur le back-office administrateur.

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3 |
| Sécurité | Spring Security (rôles) |
| Persistance | Spring Data JPA + PostgreSQL |
| Vues | Thymeleaf |
| Stockage médias | Cloudinary |
| Hébergement | Render / Railway / Fly.io |
| Build | Maven |
| Java | 17+ |

## Rôles utilisateurs

- **Visiteur** — navigation publique, galerie, profils créateurs
- **Créateur** — gestion du profil, albums, upload, réception des demandes
- **Client** — envoi de demandes, suivi, favoris, avis
- **Administrateur** — validation des comptes, modération, statistiques

## Documentation

- [Fonctionnalités détaillées](docs/fonctionnalites.md)
- [Plan de release](docs/plan-de-release.md)
- [Wireframes](docs/wireframes.md)

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
