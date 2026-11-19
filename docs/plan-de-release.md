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
- [ ] Confirmer l'hébergement avec l'enseignant (VM école ou Render pour le backend et la base PostgreSQL, Vercel pour le frontend, reliés par CORS) (cf. #5)
- [ ] Ajouter `Dockerfile`, `compose.yaml`, `.env.example` et `.gitignore` sur la branche `chore/deploiement-initial`
- [ ] Déployer un backend Spring Boot vide (répondant à `GET /actuator/health → 200`)
- [ ] Compléter la section Installation et lancement du README

---

### v0.1 (MVP) — Séances 2 à 4
**Objectif :** Une application consultable avec profils, albums et galerie filtrée.

> En v0.1, les comptes créateurs sont activés automatiquement. La validation manuelle par l'admin est introduite en v0.2. Des données de départ (créateurs et albums) sont ajoutées en S4 pour les tests.

#### Séance 2
- [x] **(Gedeon)** Inscription et connexion des utilisateurs
- [x] **(Gedeon)** Gestion des rôles client et créateur
- [x] **(Francisco)** Création et modification du profil créateur (bio, ville, pays, zones d'intervention)

#### Séance 3
- [x] **(Francisco)** Galerie publique avec liste des créateurs
- [x] **(Francisco)** Filtres : type de prestation, ville, pays, type de média
- [x] **(Francisco)** Création d'albums et upload de photos (suppression et vidéos reportées en v0.2, #94)
- [x] **(Gedeon)** Protection des routes par rôle (API et frontend), gestion de la session côté React

#### Séance 4
- [x] **(Francisco)** Profil public d'un créateur (bio, ville, pays, zones, albums ; les tarifs indicatifs sont remplacés par les prestations à prix fixe en v0.2, #27)
- [x] **(Les deux)** Données de départ pour les tests (quelques créateurs et albums pré-remplis)
- [x] **Déploiement v0.1 + tests**

---

### v0.2 — Séances 5 à 7
**Objectif :** Le flux de réservation complet avec agenda et anti-collision.

#### Séance 5
- [ ] **(Francisco)** Création de prestations à prix fixe par le créateur (nom, durée, prix, options payantes), affichées sur le profil public à la place des tarifs indicatifs (#27)
- [ ] **(Francisco)** Gestion des disponibilités : le créateur définit ses créneaux libres et son préavis minimum
- [ ] **(Francisco)** Reporté de la v0.1 : suppression de photos et d'albums, ajout de vidéos (#94) ; choix des types de prestation dans « Mon profil » (#87)
- [ ] **(Les deux)** Reporté de la v0.1 : exclusion des créateurs suspendus de la galerie sans charger tous les créateurs validés (#98 : méthode `UserApi` par Gedeon, puis `catalog` par Francisco)
- [ ] **(Gedeon)** Interface de réservation : le client choisit une prestation et un créneau disponible
- [ ] **(Gedeon)** Anti-collision : un créneau confirmé n'est plus réservable ; marge entre deux rendez-vous (s'appuie sur les disponibilités exposées par `offerings`)

#### Séance 6
- [ ] **(Gedeon)** API d'acceptation et de refus des demandes reçues (module `bookings`)
- [ ] **(Francisco)** Tableau de bord créateur (page) : demandes reçues, boutons accepter ou refuser
- [ ] **(Francisco)** Validation des comptes créateurs par l'administrateur

#### Séance 7
- [ ] **(Gedeon)** Tableau de bord client : suivi des statuts (En attente, Confirmée, Refusée, Annulée, Expirée, Terminée)
- [ ] **(Gedeon)** Annulation par le client (gratuite jusqu'à 48 h avant, marquée « tardive » après)
- [ ] **(Gedeon)** Expiration automatique : demande non traitée après 7 jours → statut Expirée, créneau libéré
- [ ] **(Gedeon)** API pour marquer une prestation comme terminée
- [ ] **(Francisco)** Bouton « Marquer comme terminée » sur le tableau de bord créateur
- [ ] **(Gedeon)** Journal des changements de statut (qui, quand, de quel statut vers quel statut)
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
- [ ] **(Francisco)** Statistiques publiques de la page d'accueil : créateurs actifs, prestations terminées, note moyenne, délai de réponse (#76, reporté de la galerie v0.1 : dépend de `offerings` et de `reputation`)
- [ ] **Déploiement v0.3 + tests**

---

### v1.0 — Séances 10 à 11
**Objectif :** Version finale prête pour la livraison.

#### Séance 10
- [ ] **(Gedeon)** API de traitement des signalements (module `moderation`)
- [ ] **(Francisco)** Back-office admin : écran de traitement des signalements (médias et avis)
- [ ] **(Francisco)** Gestion des catégories de prestation
- [ ] **(Gedeon)** Badges automatiques sur les profils créateurs (Nouveau, Top créateur, Réponse rapide)
- [ ] **(Francisco)** Statistiques admin (nombre d'utilisateurs par rôle, réservations par statut, créateurs validés / en attente)

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

Le travail est réparti **par module** du monolithe modulaire : chaque module a un seul responsable, qui en écrit le code backend et les pages frontend associées. L'autre membre relit les pull requests. Les modules communiquent uniquement via leur API publique ou par événements (vérifié par `ModularityTests`), ce qui évite de modifier les mêmes fichiers en parallèle.

| Module | Responsable | Contenu |
|--------|-------------|---------|
| `users` | Dassi Kouam Gedeon | Inscription, connexion, rôles, sécurité |
| `bookings` | Dassi Kouam Gedeon | Réservation simple et multiple, total et promo, acceptation et refus, anti-collision, annulation, expiration, statut Terminée, journal des changements de statut |
| `reputation` | Dassi Kouam Gedeon | Avis, notes, badges automatiques |
| `moderation` | Dassi Kouam Gedeon | Signalements des avis et des médias |
| `creators` | Derouck Arredondo Francisco Homero | Profil créateur (bio, pays, zones), albums, upload des médias, statut de validation |
| `offerings` | Derouck Arredondo Francisco Homero | Prestations à prix fixe, options payantes, agenda, disponibilités, préavis minimum, marge entre rendez-vous |
| `catalog` | Derouck Arredondo Francisco Homero | Galerie publique, filtres, gestion des catégories |
| `admin` | Derouck Arredondo Francisco Homero | Back-office : validation des créateurs, écran de traitement des signalements, statistiques |
| `notifications` | Derouck Arredondo Francisco Homero | Réactions aux événements publiés par les autres modules |

**Pages frontend (wireframes)** : Gedeon — 02, 03, 05, 06, 09 ; Francisco — 01, 04, 07, 08, 10, 11, 12.

**En commun** : données de départ, déploiement, tests complets et documentation finale.

---

## Suivi des releases

- Le projet suit **GitHub Flow** : une branche courte par issue, partie de `main` et nommée `<type>/<n°>-<description>`, puis une pull request vers `main` contenant `Closes #<n°>`, relue par l'autre membre. Le détail est dans le [README](../README.md#workflow-git-github-flow).
- Chaque issue et chaque PR porte un label **`release:<version>`** (`release:cadrage`, `release:v0.1`, …, `release:v1.0`). Ce label remplace les milestones comme moyen de rattacher une tâche à sa release.
- Une release est livrée quand sa Definition of Done est remplie. Elle reçoit alors un **tag** sur `main` (`v0.1`, `v0.2`, `v0.3`, `v1.0`).

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
| 2026-10-05 | Cadrage S1 | Décision hébergement : VM école prioritaire, sinon Render + Neon + Cloudflare Pages (tout gratuit, sans CB) ; ajout des tâches de déploiement initial en S1 | Contrainte projet : déploiement en ligne dès S1, zéro dépense |
| 2026-10-12 | Toutes | Répartition du travail **par module** (un responsable par module) au lieu de par fonctionnalité : la galerie, les filtres, les catégories et les statistiques admin passent à Francisco ; l'acceptation et le refus, l'anti-collision, l'expiration, le statut Terminée et le journal des statuts passent à Gedeon | Le module `bookings` était partagé entre les deux membres, ce qui créait des conflits et brouillait l'historique des contributions |
| 2026-10-12 | Toutes | Passage de la convention `main` / `develop` / `feature/*` au workflow **GitHub Flow** (branches courtes liées aux issues, PR vers `main`, tag par release) et suivi des tâches dans **GitHub Projects** | Workflow imposé par les consignes du cours ; la branche `develop` a été supprimée |
| 2026-10-19 | Toutes | **Milestones remplacés par des labels `release:<version>`** sur toutes les issues et PRs ; ajout du workflow `pr-labels.yml` (vérifie la présence du label sur chaque PR) | Demande du professeur |
| 2026-10-19 | Cadrage S1 | Hébergement du frontend : **Cloudflare Pages remplacé par Render** (Static Sites) — cf. [architecture-c4.md](architecture/architecture-c4.md) et issue #60 en cours | Cohérence avec la décision d'infrastructure ; #60 tranche si le frontend est finalement servi par Spring Boot |
| 2026-11-11 | Cadrage S1 | Frontend servi par Spring Boot : build React embarqué dans le `.jar`, **un seul service Render** au lieu d'un Static Site séparé (#60) | Render Service Previews ne crée qu'un aperçu par service : un seul service montre l'API et le frontend ensemble |
| 2026-11-12 | v0.1 → v0.2 et v0.3 | Ajustements de périmètre de la v0.1 : suppression de photos et d'albums, et vidéos → v0.2 (#94) ; choix des types de prestation dans « Mon profil » → v0.2 (#87) ; tarifs indicatifs du profil public → v0.2, avec les prestations à prix fixe (#27) ; statistiques de l'accueil → v0.3 (#76) ; optimisation de l'exclusion des créateurs suspendus → v0.2 (#98) | Livrer à temps une v0.1 testée : ces points dépendent de fonctionnalités prévues plus tard (prestations, réputation) ou ne bloquent pas l'usage de la v0.1 |
| 2026-11-16 | Cadrage S1 | Hébergement : **frontend sur Vercel, backend seul sur Render**, reliés par CORS (#5, #106, #107, #108, #109) ; remplace la décision du 11/11 (frontend embarqué dans le `.jar`, #60) | Demande de l'enseignant (deux origines distinctes) |
