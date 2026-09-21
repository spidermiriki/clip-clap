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

### 4.4 Statistiques
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
- Un créateur non validé ou suspendu n'apparaît pas dans la galerie et ses créneaux sont inaccessibles
- Une prestation liée à une réservation confirmée ne peut pas être supprimée
- Deux clients ne peuvent pas confirmer le même créneau simultanément (contrainte d'unicité)
- Un journal des changements de statut est conservé (qui, quand, de quel statut vers quel statut)
