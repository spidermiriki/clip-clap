# Wireframes

Représentations textuelles des écrans principaux de l'application.
Ces wireframes servent de référence pour l'implémentation Thymeleaf.

---

## 1. Accueil / Galerie publique

```
+----------------------------------------------------------+
|  CLIC & CLAP          [Connexion]  [Inscription]         |
+----------------------------------------------------------+
|                                                          |
|  Trouvez le créateur idéal pour votre projet             |
|                                                          |
|  +---------------+  +------------+  +----------------+  |
|  | Type           |  | Ville      |  | Média          |  |
|  | [Mariage    v] |  | [Paris   ] |  | [Photo/Vidéo v]|  |
|  +---------------+  +------------+  +----------------+  |
|                          [Rechercher]                    |
|                                                          |
|  --- Résultats (24 créateurs) ---                        |
|                                                          |
|  +----------+  +----------+  +----------+               |
|  | [photo]  |  | [photo]  |  | [photo]  |               |
|  | Jean D.  |  | Marie L. |  | Paul K.  |               |
|  | Paris    |  | Lyon     |  | Bruxelles|               |
|  | Mariage  |  | Sport    |  | Événement|               |
|  | ★★★★☆   |  | ★★★★★   |  | ★★★☆☆   |               |
|  | [Voir]   |  | [Voir]   |  | [Voir]   |               |
|  +----------+  +----------+  +----------+               |
|                                                          |
|              [< 1  2  3 ... >]                           |
+----------------------------------------------------------+
```

---

## 2. Connexion

```
+----------------------------------------------------------+
|  CLIC & CLAP                                             |
+----------------------------------------------------------+
|                                                          |
|              Connexion à votre compte                    |
|                                                          |
|         Email :   [________________________]             |
|         Mot de passe : [____________________]  [Voir]    |
|                                                          |
|                   [  Se connecter  ]                     |
|                                                          |
|         Pas encore de compte ? [S'inscrire]              |
|                                                          |
+----------------------------------------------------------+
```

---

## 3. Inscription

```
+----------------------------------------------------------+
|  CLIC & CLAP                                             |
+----------------------------------------------------------+
|                                                          |
|                  Créer un compte                         |
|                                                          |
|  Je suis un :  ( ) Client      ( ) Créateur              |
|                                                          |
|  Prénom :      [________________________]                |
|  Nom :         [________________________]                |
|  Email :       [________________________]                |
|  Mot de passe: [________________________]                |
|  Confirmation: [________________________]                |
|                                                          |
|  [v] J'accepte les conditions d'utilisation              |
|                                                          |
|                 [  S'inscrire  ]                         |
|                                                          |
|  Déjà un compte ? [Se connecter]                         |
|                                                          |
+----------------------------------------------------------+
```

---

## 4. Profil public d'un créateur

```
+----------------------------------------------------------+
|  CLIC & CLAP        [Tableau de bord]  [Déconnexion]     |
+----------------------------------------------------------+
|                                                          |
|  [PHOTO]   Jean Dupont                  [♥ Favoris]      |
|            Paris  |  Mariage, Événement                  |
|            ★★★★☆ (18 avis)                               |
|                                                          |
|  À propos : Photographe professionnel depuis 10 ans...   |
|                                                          |
|  Tarifs indicatifs : à partir de 300 €/journée           |
|                                                          |
|  [  Envoyer une demande de prestation  ]                 |
|                                                          |
|  --- Albums ---                                          |
|  [Mariages 2024]  [Portraits]  [Corporate]               |
|                                                          |
|  +------+ +------+ +------+ +------+ +------+           |
|  |  img | |  img | |  img | |  img | |  img |           |
|  +------+ +------+ +------+ +------+ +------+           |
|                                                          |
|  --- Avis clients ---                                    |
|  Sophie M. — ★★★★★ — "Excellent photographe, très pro"  |
|  Lucas T.  — ★★★★☆ — "Beau travail, livraison rapide"   |
|                                                          |
+----------------------------------------------------------+
```

---

## 5. Gestion des albums (créateur)

```
+----------------------------------------------------------+
|  CLIC & CLAP — Mon espace créateur                       |
+----------------------------------------------------------+
|  [Mon profil] [Mes albums] [Mes demandes] [Déconnexion]  |
+----------------------------------------------------------+
|                                                          |
|  Mes albums                    [+ Créer un album]        |
|                                                          |
|  +-----------------------------+                         |
|  | Mariages 2024     [Modifier][Supprimer]               |
|  | 12 photos                                             |
|  | [+ Ajouter une photo] [+ Ajouter une vidéo]           |
|  |                                                       |
|  | +----+ +----+ +----+ +----+                           |
|  | |img | |img | |img | |[X] |                           |
|  | +----+ +----+ +----+ +----+                           |
|  +-----------------------------+                         |
|                                                          |
|  +-----------------------------+                         |
|  | Portraits          [Modifier][Supprimer]              |
|  | 5 photos                                              |
|  +-----------------------------+                         |
|                                                          |
+----------------------------------------------------------+
```

---

## 6. Formulaire de demande de prestation (client)

```
+----------------------------------------------------------+
|  CLIC & CLAP                                             |
+----------------------------------------------------------+
|                                                          |
|  Envoyer une demande à Jean Dupont                       |
|                                                          |
|  Date souhaitée :    [  JJ/MM/AAAA  ]                   |
|  Lieu :              [________________________]          |
|  Budget indicatif :  [_________] €                       |
|  Type de prestation: [Mariage              v]            |
|                                                          |
|  Description :                                           |
|  +--------------------------------------------------+    |
|  |                                                  |    |
|  |                                                  |    |
|  +--------------------------------------------------+    |
|                                                          |
|          [Annuler]     [  Envoyer la demande  ]          |
|                                                          |
+----------------------------------------------------------+
```

---

## 7. Tableau de bord créateur (demandes reçues)

```
+----------------------------------------------------------+
|  CLIC & CLAP — Tableau de bord créateur                  |
+----------------------------------------------------------+
|  [Mon profil] [Mes albums] [Mes demandes] [Déconnexion]  |
+----------------------------------------------------------+
|                                                          |
|  Résumé :  3 nouvelles  |  5 acceptées  |  2 refusées   |
|                                                          |
|  --- Demandes reçues ---                                 |
|                                                          |
|  +--------------------------------------------------+    |
|  | Sophie M. — Mariage — 14/06/2025 — Paris         |    |
|  | Budget : 800 €   Statut : [ENVOYÉE]               |    |
|  | [Voir détail]  [Accepter]  [Refuser]  [Devis]     |    |
|  +--------------------------------------------------+    |
|                                                          |
|  +--------------------------------------------------+    |
|  | Lucas T. — Événement — 22/07/2025 — Lyon          |    |
|  | Budget : 400 €   Statut : [ACCEPTÉE]              |    |
|  | [Voir détail]                                     |    |
|  +--------------------------------------------------+    |
|                                                          |
|  --- Mes avis (note moyenne : ★★★★☆ 4.2/5) ---          |
|  Sophie M. — ★★★★★ — "Parfait !"                        |
|                                                          |
+----------------------------------------------------------+
```

---

## 8. Tableau de bord client (mes demandes et favoris)

```
+----------------------------------------------------------+
|  CLIC & CLAP — Mon espace client                         |
+----------------------------------------------------------+
|  [Mes demandes]  [Mes favoris]  [Déconnexion]            |
+----------------------------------------------------------+
|                                                          |
|  --- Mes demandes ---                                    |
|                                                          |
|  +--------------------------------------------------+    |
|  | Jean Dupont — Mariage — 14/06/2025 — Paris        |    |
|  | Statut : [ACCEPTÉE]                               |    |
|  | [Voir le devis]                                   |    |
|  +--------------------------------------------------+    |
|                                                          |
|  +--------------------------------------------------+    |
|  | Marie Laurent — Sport — 10/05/2025 — Lyon         |    |
|  | Statut : [TERMINÉE]                               |    |
|  | [Laisser un avis]                                 |    |
|  +--------------------------------------------------+    |
|                                                          |
|  --- Mes favoris ---                                     |
|                                                          |
|  +----------+  +----------+  +----------+               |
|  | [photo]  |  | [photo]  |  | [photo]  |               |
|  | Jean D.  |  | Marie L. |  | Paul K.  |               |
|  | [♥ Retirer]| | [♥ Retirer]| |[♥ Retirer]|            |
|  +----------+  +----------+  +----------+               |
|                                                          |
+----------------------------------------------------------+
```

---

## 9. Back-office administrateur

```
+----------------------------------------------------------+
|  CLIC & CLAP — Administration                            |
+----------------------------------------------------------+
|  [Créateurs]  [Modération]  [Catégories]  [Stats]        |
+----------------------------------------------------------+
|                                                          |
|  --- Comptes créateurs en attente (3) ---                |
|                                                          |
|  +--------------------------------------------------+    |
|  | Alice B. — alice@mail.com — Inscrit le 20/09/25  |    |
|  | Spécialités : Mariage, Portrait                   |    |
|  | [Voir le profil]  [Valider]  [Refuser]            |    |
|  +--------------------------------------------------+    |
|                                                          |
|  --- Contenus signalés (2) ---                           |
|                                                          |
|  +--------------------------------------------------+    |
|  | Avis de Lucas T. sur Jean D. — signalé le 18/09  |    |
|  | "Contenu inapproprié"                             |    |
|  | [Voir]  [Supprimer]  [Lever le signalement]       |    |
|  +--------------------------------------------------+    |
|                                                          |
|  --- Statistiques ---                                    |
|  Utilisateurs : 142  |  Créateurs actifs : 38           |
|  Demandes totales : 215  |  En attente : 12             |
|                                                          |
+----------------------------------------------------------+
```

---

## Navigation par rôle

| Écran | Visiteur | Client | Créateur | Admin |
|-------|----------|--------|----------|-------|
| Galerie publique | ✓ | ✓ | ✓ | ✓ |
| Profil public | ✓ | ✓ | ✓ | ✓ |
| Inscription / Connexion | ✓ | — | — | — |
| Formulaire de demande | — | ✓ | — | — |
| Tableau de bord client | — | ✓ | — | — |
| Gestion profil créateur | — | — | ✓ | — |
| Gestion albums | — | — | ✓ | — |
| Tableau de bord créateur | — | — | ✓ | — |
| Back-office admin | — | — | — | ✓ |
