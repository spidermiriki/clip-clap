# Plan de release

## Vue d'ensemble

| Version | Séances | Contenu | Livraison |
|---------|---------|---------|-----------|
| **Cadrage** | S1 | Nom, docs, wireframes, dépôt GitHub | S1 |
| **v0.1 (MVP)** | S2 à S4 | Authentification et rôles, profil créateur avec pays et zones, albums et photos, galerie avec filtres | S4 |
| **v0.2** | S5 à S7 | Prestations à prix fixe, agenda et disponibilités, réservation d'un créneau sans collision, statuts, tableaux de bord, validation des créateurs, annulation et expiration automatique | S7 |
| **v0.3** | S8 à S9 | Réservation multiple avec total et promo dégressive, options payantes, avis et notes | S9 |
| **v1.0** | S10 à S11 | Modération et signalement, gestion des catégories, badges automatiques, tests complets, documentation finale | S11 |
| **Bonus** (si le temps le permet) | — | Favoris, vidéos par lien, recherche avancée | — |

---

## Détail par séance

### Cadrage — Séance 1
**Objectif :** Poser les bases du projet avant tout développement.

- [ ] Choisir le nom du projet et créer le dépôt GitHub privé
- [ ] Ajouter l'enseignant comme collaborateur
- [ ] Rédiger la documentation initiale (fonctionnalités, plan de release, wireframes, tests)
- [ ] Mettre en place la convention de branches et les règles de travail en binôme
- [ ] Déployer l'application en ligne (environnement vide)
- [ ] Compléter la section Installation et lancement du README

---

### v0.1 (MVP) — Séances 2 à 4
**Objectif :** Une application consultable avec profils, albums et galerie filtrée.

> En v0.1, les comptes créateurs sont activés automatiquement. La validation manuelle par l'admin est introduite en v0.2. Des données de départ (créateurs et albums) sont ajoutées en S4 pour les tests.

#### Séance 2
- [ ] **(Gedeon)** Inscription et connexion des utilisateurs
- [ ] **(Gedeon)** Gestion des rôles client et créateur
- [ ] **(Francisco)** Création et modification du profil créateur (bio, ville, pays, zones d'intervention)

#### Séance 3
- [ ] **(Gedeon)** Galerie publique avec liste des créateurs
- [ ] **(Gedeon)** Filtres : type de prestation, ville, pays, type de média
- [ ] **(Francisco)** Création d'albums et upload de photos

#### Séance 4
- [ ] **(Francisco)** Profil public d'un créateur (bio, albums, pays, zones, tarifs indicatifs)
- [ ] **(Les deux)** Données de départ pour les tests (quelques créateurs et albums pré-remplis)
- [ ] **Déploiement v0.1 + tests**

---

### v0.2 — Séances 5 à 7
**Objectif :** Le flux de réservation complet avec agenda et anti-collision.

#### Séance 5
- [ ] **(Francisco)** Création de prestations à prix fixe par le créateur (nom, durée, prix, options payantes)
- [ ] **(Francisco)** Gestion des disponibilités : le créateur définit ses créneaux libres et son préavis minimum
- [ ] **(Gedeon)** Interface de réservation : le client choisit une prestation et un créneau disponible

#### Séance 6
- [ ] **(Francisco)** Tableau de bord créateur : demandes reçues, accepter ou refuser
- [ ] **(Francisco)** Anti-collision : un créneau confirmé n'est plus réservable ; marge entre deux rendez-vous
- [ ] **(Francisco)** Validation des comptes créateurs par l'administrateur

#### Séance 7
- [ ] **(Gedeon)** Tableau de bord client : suivi des statuts (En attente, Confirmée, Refusée, Annulée, Expirée, Terminée)
- [ ] **(Gedeon)** Annulation par le client (gratuite jusqu'à 48 h avant, marquée « tardive » après)
- [ ] **(Francisco)** Expiration automatique : demande non traitée après 7 jours → statut Expirée, créneau libéré
- [ ] **(Francisco)** Marquer une prestation comme terminée (déclenchée par le créateur)
- [ ] **(Les deux)** Journal des changements de statut (qui, quand, de quel statut vers quel statut)
- [ ] **Déploiement v0.2 + tests**

---

### v0.3 — Séances 8 à 9
**Objectif :** Réservations avancées et réputation.

#### Séance 8
- [ ] **(Gedeon)** Réservation multiple : sélectionner plusieurs créneaux en une seule fois
- [ ] **(Gedeon)** Total calculé automatiquement (prestations + options)
- [ ] **(Gedeon)** Promo dégressive : 5 % dès 2 rendez-vous, 10 % dès 4 — affiché uniquement, sans paiement en ligne
- [ ] **(Gedeon)** Atomicité : si un créneau du pack n'est plus libre, toute la réservation est refusée

#### Séance 9
- [ ] **(Gedeon)** Avis et notes (texte + note de 1 à 5, dans les 30 jours suivant le statut Terminée)
- [ ] **(Gedeon)** Bouton « Signaler » sur les avis et les médias
- [ ] **Déploiement v0.3 + tests**

---

### v1.0 — Séances 10 à 11
**Objectif :** Version finale prête pour la livraison.

#### Séance 10
- [ ] **(Les deux)** Back-office admin : traitement des signalements (médias et avis)
- [ ] **(Les deux)** Gestion des catégories de prestation
- [ ] **(Les deux)** Badges automatiques sur les profils créateurs (Nouveau, Top créateur, Réponse rapide)
- [ ] **(Les deux)** Statistiques admin (nombre d'utilisateurs par rôle, réservations par statut, créateurs validés / en attente)

#### Séance 11
- [ ] Tests complets
- [ ] Corrections de bugs
- [ ] Documentation finale mise à jour
- [ ] **Déploiement v1.0 + validation finale**

---

## Definition of Done

Pour chaque release, une version est considérée comme livrée si et seulement si :

1. Toutes les fonctionnalités prévues sont **opérationnelles**
2. Les **tests** correspondants sont réalisés (voir [docs/tests.md](tests.md))
3. L'application est **déployée en ligne** et accessible

---

## Répartition des responsabilités

| Fonctionnalité | Responsable principal |
|----------------|----------------------|
| Profil créateur (pays, zones, bio) | Derouck Arredondo Francisco Homero |
| Albums et upload de médias | Derouck Arredondo Francisco Homero |
| Prestations à prix fixe et agenda | Derouck Arredondo Francisco Homero |
| Anti-collision et marge entre RDV | Derouck Arredondo Francisco Homero |
| Expiration automatique des demandes | Derouck Arredondo Francisco Homero |
| Validation des créateurs (admin) | Derouck Arredondo Francisco Homero |
| Inscription, connexion, rôles | Dassi Kouam Gedeon |
| Galerie publique et filtres | Dassi Kouam Gedeon |
| Interface de réservation client | Dassi Kouam Gedeon |
| Réservation multiple, total, promo | Dassi Kouam Gedeon |
| Annulation et suivi des statuts | Dassi Kouam Gedeon |
| Avis, notes et signalement | Dassi Kouam Gedeon |
| Back-office admin complet | Les deux (croisement) |
| Badges automatiques | Les deux |
| Déploiement | Les deux |

---

## Convention de branches

```
main              ← production stable (merge après chaque release)
develop           ← intégration continue
feature/<nom>     ← une branche par fonctionnalité
```

**Règle :** toute `feature/*` est mergée via **pull request** relue et approuvée par l'autre membre du binôme.

---

## Journal des modifications

| Date | Version concernée | Modification | Raison |
|------|-------------------|--------------|--------|
| 2026-09-21 | Toutes | Remplacement du modèle « demande + devis libre » par des **prestations à prix fixe** créées par le créateur | Permet un total calculable, une promo de pack cohérente et clarifie le cycle de vie des statuts |
| 2026-09-21 | v0.2 | Ajout d'un **agenda avec anti-collision** (créneaux libres, préavis minimum, marge entre rendez-vous) | Fonctionnalité centrale pour un vrai outil de réservation |
| 2026-09-21 | v0.1 | Ajout du **pays et des zones d'intervention** sur le profil créateur, filtre correspondant dans la galerie | Différenciation des créateurs à l'international |
| 2026-09-21 | v0.3 | Ajout de la **réservation multiple** avec total automatique et promo dégressive (5 % / 10 %) | Offre une vraie valeur ajoutée par rapport à un simple formulaire |
| 2026-09-21 | v0.2 | Ajout de l'**annulation par le client** (gratuite jusqu'à 48 h, tardive après) et de l'**expiration automatique** (7 jours sans réponse) | Complète le cycle de vie d'une réservation et évite les créneaux bloqués indéfiniment |
| 2026-09-21 | v1.0 | Ajout des **badges automatiques** (Nouveau, Top créateur, Réponse rapide) | Enrichit la réputation sans écran supplémentaire |
| 2026-09-21 | Bonus | **Favoris, vidéos par lien, statistiques admin, recherche avancée** déplacés en bonus | Allègement pour tenir les délais ; ces fonctionnalités ne bloquent pas le cœur du projet |
| 2026-10-05 | v1.0 S10 | **Statistiques admin** rapatriées du Bonus vers v1.0 S10 | Cohérence avec la section 4.4 de fonctionnalites.md où elles font partie du périmètre normal |
| 2026-10-05 | v0.2 S7 | Ajout du **journal des changements de statut** (qui, quand, de quel statut vers quel statut) | Règle d'intégrité déjà documentée dans fonctionnalites.md §6 mais absente du plan de release |
