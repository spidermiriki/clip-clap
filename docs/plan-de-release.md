# Plan de release

## Vue d'ensemble

| Version | Séances | Contenu | Livraison |
|---------|---------|---------|-----------|
| **Cadrage** | S1 | Nom, docs, wireframes, dépôt GitHub, squelette Spring Boot déployé en ligne | S1 |
| **v0.1 (MVP)** | S2 à S4 | Inscription/connexion avec rôles, profil créateur, albums, upload de photos, galerie publique avec filtres | S4 |
| **v0.2** | S5 à S7 | Demandes de prestation, statuts, tableaux de bord créateur et client | S7 |
| **v0.3** | S8 à S9 | Avis et notes, favoris, vidéos par lien, recherche avancée | S9 |
| **v1.0** | S10 à S11 | Back-office admin, modération, tests, finitions, documentation finale | S11 |

---

## Détail par séance

### Cadrage — Séance 1
**Objectif :** Poser les bases du projet avant tout développement.

- [ ] Choisir le nom du projet et créer le dépôt GitHub privé
- [ ] Ajouter l'enseignant comme collaborateur
- [ ] Rédiger `README.md`, `docs/fonctionnalites.md`, `docs/plan-de-release.md`, `docs/wireframes.md`
- [ ] Initialiser le squelette Spring Boot (pom.xml, structure de packages)
- [ ] Déployer le squelette vide en ligne (Render / Railway / Fly.io)
- [ ] Configurer la base de données PostgreSQL en ligne
- [ ] Mettre en place la convention de branches (`main`, `develop`, `feature/*`)

---

### v0.1 (MVP) — Séances 2 à 4
**Objectif :** Une application utilisable avec les fonctions de base.

#### Séance 2
- [ ] Entités JPA : `User`, `CreatorProfile`, `Category`
- [ ] Spring Security : inscription, connexion, déconnexion
- [ ] Gestion des rôles : CLIENT, CREATOR, ADMIN

#### Séance 3
- [ ] Entités JPA : `Album`, `Media`
- [ ] Interface créateur : création/modification du profil
- [ ] Création d'albums et upload de photos (Cloudinary)

#### Séance 4
- [ ] Galerie publique avec liste des créateurs
- [ ] Filtres : type de prestation, ville, type de média
- [ ] Profil public d'un créateur (bio, albums, tarifs)
- [ ] **Déploiement v0.1 + tests**

---

### v0.2 — Séances 5 à 7
**Objectif :** Le flux de prestation complet entre client et créateur.

#### Séance 5
- [ ] Entité JPA : `ServiceRequest`
- [ ] Formulaire de demande de prestation (client)
- [ ] Envoi de la demande à un créateur

#### Séance 6
- [ ] Tableau de bord créateur : liste des demandes reçues
- [ ] Actions créateur : accepter, refuser, envoyer un devis

#### Séance 7
- [ ] Tableau de bord client : suivi des statuts des demandes
- [ ] **Déploiement v0.2 + tests**

---

### v0.3 — Séances 8 à 9
**Objectif :** Enrichir l'expérience utilisateur.

#### Séance 8
- [ ] Entités JPA : `Review`, `Favorite`
- [ ] Système d'avis et de notes (client → créateur après prestation terminée)
- [ ] Favoris : ajouter/retirer, liste des favoris

#### Séance 9
- [ ] Ajout de vidéos par lien YouTube / Vimeo
- [ ] Recherche avancée (combinaison de filtres)
- [ ] **Déploiement v0.3 + tests**

---

### v1.0 — Séances 10 à 11
**Objectif :** Version finale prête pour la livraison.

#### Séance 10
- [ ] Back-office administrateur : validation des comptes créateurs
- [ ] Modération : contenus et avis signalés
- [ ] Gestion des catégories et statistiques

#### Séance 11
- [ ] Tests d'intégration et tests manuels complets
- [ ] Corrections de bugs
- [ ] Documentation finale mise à jour
- [ ] **Déploiement v1.0 + validation finale**

---

## Definition of Done

Pour chaque release, une version est considérée comme livrée si et seulement si :

1. Toutes les fonctionnalités prévues sont **opérationnelles**
2. Les **tests** correspondants sont réalisés (manuels et/ou automatisés)
3. L'application est **déployée en ligne** et accessible

---

## Répartition des responsabilités

| Fonctionnalité | Responsable principal |
|----------------|----------------------|
| Profil créateur | Derouck Arredondo Francisco Homero |
| Albums et upload de médias | Derouck Arredondo Francisco Homero |
| Demandes de prestation | Dassi Kouam Gedeon |
| Avis et notes | Dassi Kouam Gedeon |
| Favoris | Dassi Kouam Gedeon |
| Back-office admin | Les deux (croisement) |
| Spring Security / rôles | Les deux |
| Déploiement | Les deux |

---

## Convention de branches

```
main              ← production stable (merge après chaque release)
develop           ← intégration continue
feature/<nom>     ← une branche par fonctionnalité
```

**Règle :** toute `feature/*` est mergée via **pull request** relue et approuvée par l'autre membre du binôme.
