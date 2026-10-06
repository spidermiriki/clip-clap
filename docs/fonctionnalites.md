# Fonctionnalités détaillées

## 1. Visiteur (non connecté)

### 1.1 Galerie publique
- Afficher la liste de tous les créateurs actifs
- Filtrer par **type de prestation** : mariage, événementiel, sport, produit, clip, etc.
- Filtrer par **ville**
- Filtrer par **pays**
- Filtrer par **type de média** : photo ou vidéo
- Combiner plusieurs filtres simultanément

### 1.2 Profil public d'un créateur
- Voir la bio, la ville, le pays et les zones d'intervention
- Voir les albums avec leurs photos
- Voir les prestations proposées (nom, durée, prix, options)
- Voir les badges du créateur (Nouveau, Top créateur, Réponse rapide)
- Voir les avis et la note moyenne laissés par les clients

### 1.3 Inscription et connexion
- S'inscrire en tant que **client** ou **créateur** (choix du rôle à l'inscription)
- Se connecter avec email et mot de passe
- Se déconnecter

---

## 2. Créateur (connecté)

> Compte soumis à validation par l'administrateur avant activation complète (fonctionnalité introduite en v0.2). En v0.1, l'activation est automatique.

### 2.1 Gestion du profil
- Renseigner et modifier : bio, ville, pays, zones d'intervention
- Choisir un ou plusieurs types de prestation proposés

### 2.2 Albums et médias
- Créer, renommer et supprimer des albums
- Uploader des photos dans un album
- Supprimer un média

### 2.3 Prestations à prix fixe
- Créer une prestation avec : nom, durée, prix de base
- Ajouter des **options payantes** à une prestation (ex. retouches avancées, drone, livraison express), chacune avec son supplément
- Modifier ou supprimer une prestation *(impossible si une réservation confirmée y est liée)*

### 2.4 Gestion de l'agenda et des disponibilités
- Définir des **créneaux libres** sur un calendrier
- Configurer un **préavis minimum** : aucune réservation acceptée à moins de X heures/jours de la date
- Configurer une **marge tampon** entre deux rendez-vous (trajet, matériel)
- Un créneau confirmé est automatiquement retiré du calendrier

### 2.5 Gestion des réservations reçues
- Voir la liste de toutes les réservations avec leur statut
- Consulter le détail d'une réservation (prestation choisie, créneau, options, client)
- **Confirmer** une réservation → statut passe à « Confirmée »
- **Refuser** une réservation (avec message optionnel) → statut passe à « Refusée »
- **Marquer une prestation comme terminée** une fois le travail livré → statut passe à « Terminée » et déverrouille l'avis pour le client

### 2.6 Tableau de bord
- Vue synthétique : nombre de réservations par statut
- Taux d'acceptation et délai moyen de réponse (données sources des badges)
- Liste des avis reçus et note globale

---

## 3. Client (connecté)

### 3.1 Réservation d'une prestation
- Choisir une prestation proposée par un créateur
- Sélectionner un **créneau disponible** dans l'agenda du créateur
- Ajouter des options payantes
- Voir le **total calculé automatiquement** (prix de base + options)
- Soumettre la réservation → statut initial « En attente »

### 3.2 Réservation multiple (pack)
- Sélectionner plusieurs créneaux chez le même créateur en une seule fois
- Total mis à jour en temps réel avec la **promo dégressive** :
  - 5 % de réduction à partir de 2 réservations
  - 10 % de réduction à partir de 4 réservations
- Le total est affiché à titre informatif (aucun paiement en ligne)
- **Atomicité** : si l'un des créneaux n'est plus disponible au moment de la confirmation, toute la réservation est refusée

### 3.3 Cycle de vie d'une réservation

| Statut | Déclencheur |
|--------|-------------|
| En attente | Le client soumet la réservation |
| Confirmée | Le créateur accepte |
| Refusée | Le créateur refuse |
| Annulée | Le client annule (voir règle ci-dessous) |
| Expirée | Le créateur ne répond pas dans les 7 jours |
| Terminée | Le créateur marque la prestation comme livrée |

**Règle d'annulation :** le client peut annuler gratuitement jusqu'à 48 h avant le créneau. Passé ce délai, l'annulation est enregistrée comme « tardive » (visible du créateur, sans pénalité financière car pas de paiement en ligne).

### 3.4 Avis et notes
- Laisser un avis (texte + note de 1 à 5) **uniquement après une prestation au statut « Terminée »**, dans un délai de 30 jours
- Un seul avis par prestation terminée
- Modifier ou supprimer son propre avis

### 3.5 Signalement
- Signaler un avis ou un média jugé inapproprié
- Le signalement est transmis à l'administrateur pour modération

---

## 4. Administrateur

### 4.1 Validation des comptes créateurs
- Voir la liste des comptes créateurs en attente de validation
- Valider ou refuser un compte (avec motif de refus)
- Un créateur non validé ou suspendu n'apparaît pas dans la galerie

### 4.2 Modération
- Voir les contenus (médias, avis) signalés par les utilisateurs
- Supprimer un contenu signalé ou lever le signalement
- Suspendre ou réactiver un compte utilisateur

### 4.3 Gestion des catégories
- Créer, modifier et supprimer les types de prestation

### 4.4 Statistiques(bonus)
- Nombre total d'utilisateurs par rôle
- Nombre de réservations par statut
- Nombre de créateurs validés / en attente

---

## 5. Badges automatiques (calculés par le système)

| Badge | Condition |
|-------|-----------|
| Nouveau | Moins de 3 prestations terminées |
| Top créateur | Note moyenne ≥ 4,5 sur au moins 10 avis |
| Réponse rapide | Délai moyen de réponse aux demandes inférieur à 24 h |

---

## 6. Règles d'intégrité# Fonctionnalités détaillées

## 1. Visiteur (non connecté)

### 1.1 Galerie publique
- Afficher la liste de tous les créateurs **visibles** (voir la règle de visibilité en 1.4)
- Filtrer par **type de prestation** : mariage, événementiel, sport, produit, clip, etc.
- Filtrer par **ville**
- Filtrer par **pays**
- Combiner plusieurs filtres simultanément
- Chaque carte affiche : nom, ville, pays, première photo du portfolio, note moyenne, nombre d'avis et badge

> Le filtre par **type de média** (photo ou vidéo) est reporté en bonus avec les vidéos par lien (voir section 7).

### 1.2 Profil public d'un créateur
- Voir la bio, la ville, le pays et les zones d'intervention
- Voir les albums avec leurs photos
- Voir les prestations proposées (nom, durée, prix, options)
- Voir les badges du créateur (Nouveau, Top créateur, Réponse rapide)
- Voir les avis et la note moyenne laissés par les clients
- Le profil d'un créateur non visible renvoie une erreur **404** (« créateur introuvable »), pour ne pas révéler l'existence du compte

### 1.3 Inscription et connexion
- S'inscrire en tant que **client** ou **créateur** (choix du rôle à l'inscription)
- Après l'inscription :
  - un **client** est redirigé vers la galerie ;
  - un **créateur** est redirigé vers « Compléter mon profil », puis vers son tableau de bord
    (avec un bandeau « Compte en attente de validation » à partir de la v0.2)
- Se connecter avec e-mail et mot de passe
- Se déconnecter

### 1.4 Règle de visibilité d'un créateur
Un créateur apparaît dans la galerie et son profil public est accessible **uniquement si** :
- son compte est **validé** par l'administrateur (statut `VALIDATED` ; automatique en v0.1) ;
- son compte n'est **pas suspendu** ;
- il a publié **au moins une photo** dans un de ses albums.

Ces données ne sont pas copiées : la galerie et le profil public les lisent à chaque visite.
Un créateur devient donc visible dès que ces trois conditions sont remplies.

---

## 2. Créateur (connecté)

> Compte soumis à validation par l'administrateur avant activation complète (fonctionnalité introduite en v0.2). En v0.1, l'activation est automatique.

### 2.1 Gestion du profil
- Renseigner et modifier : nom affiché, photo de profil, bio, ville, pays, zones d'intervention
- Choisir un ou plusieurs types de prestation proposés
- Cette page s'affiche automatiquement juste après l'inscription, puis reste accessible via « Mon profil »
- **Prévisualiser son profil public** (« Voir mon profil public »), y compris tant que le compte n'est pas validé :
  l'aperçu est réservé au créateur lui-même et affiche le bandeau « Aperçu — pas encore visible publiquement »

### 2.2 Albums et médias
- Créer, renommer et supprimer des albums
- Uploader des photos dans un album
- Supprimer un média

### 2.3 Prestations à prix fixe
- Créer une prestation avec : nom, durée, prix de base
- Ajouter des **options payantes** à une prestation (ex. retouches avancées, drone, livraison express), chacune avec son supplément
- Modifier ou supprimer une prestation *(impossible si une réservation confirmée y est liée)*

### 2.4 Gestion de l'agenda et des disponibilités
- Définir des **créneaux libres** sur un calendrier
- Configurer un **préavis minimum** : aucune réservation acceptée à moins de X heures/jours de la date
- Configurer une **marge tampon** entre deux rendez-vous (trajet, matériel)
- Un créneau confirmé est automatiquement retiré du calendrier

### 2.5 Gestion des réservations reçues
- Voir la liste de toutes les réservations avec leur statut
- Consulter le détail d'une réservation (prestation choisie, créneau, options, client)
- **Confirmer** une réservation → statut passe à « Confirmée »
- **Refuser** une réservation (avec message optionnel) → statut passe à « Refusée »
- **Marquer une prestation comme terminée** une fois le travail livré → statut passe à « Terminée » et déverrouille l'avis pour le client

### 2.6 Tableau de bord
- Vue synthétique : nombre de réservations par statut
- Taux d'acceptation et délai moyen de réponse (données sources des badges)
- Liste des avis reçus et note globale
- Tant que le compte n'est pas validé (à partir de la v0.2) : bandeau « Compte en attente de validation »
  et liste « Pour bien démarrer » (compléter le profil, créer un album, créer une prestation, définir ses créneaux)

---

## 3. Client (connecté)

### 3.1 Réservation d'une prestation
- Choisir une prestation proposée par un créateur
- Sélectionner un **créneau disponible** dans l'agenda du créateur
- Ajouter des options payantes
- Voir le **total calculé automatiquement** (prix de base + options)
- Soumettre la réservation → statut initial « En attente »

### 3.2 Réservation multiple (pack)
- Sélectionner plusieurs créneaux chez le même créateur en une seule fois
- Total mis à jour en temps réel avec la **promo dégressive** :
  - 5 % de réduction à partir de 2 réservations
  - 10 % de réduction à partir de 4 réservations
- Le total est affiché à titre informatif (aucun paiement en ligne)
- **Atomicité** : si l'un des créneaux n'est plus disponible au moment de la confirmation, toute la réservation est refusée

### 3.3 Cycle de vie d'une réservation

| Statut | Déclencheur |
|--------|-------------|
| En attente | Le client soumet la réservation |
| Confirmée | Le créateur accepte |
| Refusée | Le créateur refuse |
| Annulée | Le client annule (voir règle ci-dessous) |
| Expirée | Le créateur ne répond pas dans les 7 jours |
| Terminée | Le créateur marque la prestation comme livrée |

**Règle d'annulation :** le client peut annuler gratuitement jusqu'à 48 h avant le créneau. Passé ce délai, l'annulation est enregistrée comme « tardive » (visible du créateur, sans pénalité financière car pas de paiement en ligne).

### 3.4 Avis et notes
- Laisser un avis (texte + note de 1 à 5) **uniquement après une prestation au statut « Terminée »**, dans un délai de 30 jours
- Un seul avis par prestation terminée
- Modifier ou supprimer son propre avis

### 3.5 Signalement
- Signaler un avis ou un média jugé inapproprié (bouton visible uniquement pour les utilisateurs connectés)
- Le signalement est transmis à l'administrateur pour modération

---

## 4. Administrateur

### 4.1 Validation des comptes créateurs
- Voir la liste des comptes créateurs en attente de validation
- Valider ou refuser un compte (avec motif de refus)
- Un créateur non validé ou suspendu n'apparaît pas dans la galerie

### 4.2 Modération
- Voir les contenus (médias, avis) signalés par les utilisateurs
- Supprimer un contenu signalé ou lever le signalement
- Suspendre ou réactiver un compte utilisateur

### 4.3 Gestion des catégories
- Créer, modifier et supprimer les types de prestation
- En v0.1, les catégories de départ sont créées par script SQL ; leur gestion par l'administrateur arrive en v1.0

### 4.4 Statistiques (bonus)
- Nombre total d'utilisateurs par rôle
- Nombre de réservations par statut
- Nombre de créateurs validés / en attente

---

## 5. Badges automatiques (calculés par le système)

| Badge | Condition |
|-------|-----------|
| Nouveau | Moins de 3 prestations terminées |
| Top créateur | Note moyenne ≥ 4,5 sur au moins 10 avis |
| Réponse rapide | Délai moyen de réponse aux demandes inférieur à 24 h |

---

## 6. Règles d'intégrité

- Un créateur ne peut pas réserver ses propres prestations
- Un créateur non visible (non validé, suspendu ou sans photo publiée) n'apparaît pas dans la galerie, son profil public renvoie une 404 et ses créneaux sont inaccessibles
- Une prestation liée à une réservation confirmée ne peut pas être supprimée
- Deux clients ne peuvent pas confirmer le même créneau simultanément (contrainte d'unicité)
- Un journal des changements de statut est conservé (qui, quand, de quel statut vers quel statut)

---

## 7. Fonctionnalités bonus (si le temps le permet)

| Fonctionnalité | Profil concerné |
|----------------|-----------------|
| Favoris | Client |
| Vidéos par lien (YouTube, Vimeo) et filtre par type de média dans la galerie | Créateur, Visiteur |
| Statistiques administrateur (section 4.4) | Administrateur |
| Recherche avancée | Visiteur, Client |
| Connexion avec un compte Google (OAuth2) | Tous |

- Un créateur ne peut pas réserver ses propres prestations
- Un créateur non validé ou suspendu n'apparaît pas dans la galerie et ses créneaux sont inaccessibles
- Une prestation liée à une réservation confirmée ne peut pas être supprimée
- Deux clients ne peuvent pas confirmer le même créneau simultanément (contrainte d'unicité)
- Un journal des changements de statut est conservé (qui, quand, de quel statut vers quel statut)
