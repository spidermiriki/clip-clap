# Wireframes

Maquettes basse fidélité des écrans principaux de l'application.
Réalisées par Dassi Kouam Gedeon.

---

## 1. Galerie publique (accueil)

```
+----------------------------------------------------------+
|  CLIC & CLAP    [Accueil] [Recherche] [Connexion] [S'inscrire] |
+----------------------------------------------------------+
|                                                          |
|  [         IMAGE HERO — pleine largeur              ]   |
|                                                          |
+----------------------------------------------------------+
|                                                          |
|  Find by Categorie                                       |
|                                                          |
|  +--------+  +--------+  +--------+  +--------+         |
|  | [img]  |  | [img]  |  | [img]  |  | [img]  |         |
|  | Cat. 1 |  | Cat. 2 |  | Cat. 3 |  | Cat. 4 |         |
|  +--------+  +--------+  +--------+  +--------+         |
|                                                          |
+----------------------------------------------------------+
|                                                          |
|  Statistiques                                            |
|                                                          |
|  650+           12k+          4.9           <48h         |
|  Photographes   Commandes     Note /5       Livraison    |
|                 terminées                  en moyenne    |
|                                                          |
+----------------------------------------------------------+
|                                                          |
|  Professionnels recommandés                              |
|                                                          |
|  +----------+  +----------+  +----------+               |
|  | [photo]  |  | [photo]  |  | [photo]  |               |
|  | Nom      |  | Nom      |  | Nom      |               |
|  | Ville    |  | Ville    |  | Ville    |               |
|  | ★★★★☆   |  | ★★★★★   |  | ★★★☆☆   |               |
|  +----------+  +----------+  +----------+               |
|                                                          |
+----------------------------------------------------------+
|  [Contact]                         [Footer]              |
+----------------------------------------------------------+
```

---

## 2. Connexion

```
+----------------------------------------------------------+
|                                                          |
|                    CLIC & CLAP                           |
|                                                          |
|         [✉] Email    [_________________________]         |
|         [🔒] Mot de passe [________________] [👁]        |
|                                                          |
|                  [ Se connecter ]                        |
|                                                          |
|                        — ou —                            |
|                                                          |
|            [ Continuer avec Google ]                     |
|            [ Continuer avec Apple  ]                     |
|                                                          |
+----------------------------------------------------------+
```

> **Note :** La connexion via Google et Apple (OAuth2) est à valider
> avec l'enseignant avant implémentation.

---

## 3. Inscription client

```
+----------------------------------------------------------+
|                                                          |
|                  CRÉER UN COMPTE                         |
|                                                          |
|         Nom       [_________________________]            |
|         Prénom    [_________________________]            |
|         Email     [_________________________]            |
|         Mot de passe [____________________]              |
|                                                          |
|         Vous êtes :  (●) Client                          |
|                                                          |
|                  [ Créer mon compte ]                    |
|                                                          |
|         Déjà un compte ?  [Se connecter]                 |
|                                                          |
+----------------------------------------------------------+
```

---

## 4. Création de profil professionnel (créateur)

```
+----------------------------------------------------------+
|                                                          |
|            Créez votre profil professionnel              |
|                                                          |
|  Vous êtes :  [ Photographe ]  [ Vidéaste ]              |
|                                                          |
|  Spécialité  [__________________________ v]              |
|  Ville       [__________________________]                |
|  Pays        [__________________________]                |
|  Tarifs à partir de  [__________] €                      |
|  Description                                             |
|  +--------------------------------------------------+    |
|  |                                                  |    |
|  +--------------------------------------------------+    |
|                                                          |
|                  [ Créer mon profil ]                    |
|                                                          |
+----------------------------------------------------------+
```

---

## 5. Tableau de bord (profil connecté)

```
+----------------------------------------------------------+
|  CLIC & CLAP   [Accueil] [Demande] [Profil]              |
+----------------------------------------------------------+
|                                                          |
|  +-------------------+  +-------------------+           |
|  |   Mes demandes    |  |  Mes réservations  |           |
|  +-------------------+  +-------------------+           |
|                                                          |
|  +-------------------+                                   |
|  |   Mon portfolio   |                                   |
|  +-------------------+                                   |
|                                                          |
+----------------------------------------------------------+
```

---

## 6. Recherche / Espace professionnel

```
+----------------------------------------------------------+
|  CLIC & CLAP   [Accueil] [Recherche] [Profil]            |
+----------------------------------------------------------+
|                                                          |
|  [ 🔍 Rechercher un professionnel...              ]      |
|                                                          |
|  Catégorie                                               |
|  +--------+  +--------+  +--------+  +--------+         |
|  | [img]  |  | [img]  |  | [img]  |  | [img]  |         |
|  | Cat. 1 |  | Cat. 2 |  | Cat. 3 |  | Cat. 4 |         |
|  +--------+  +--------+  +--------+  +--------+         |
|                                                          |
|  Professionnels recommandés                              |
|  +----------+  +----------+  +----------+               |
|  | [photo]  |  | [photo]  |  | [photo]  |               |
|  | Nom      |  | Nom      |  | Nom      |               |
|  | Ville    |  | Ville    |  | Ville    |               |
|  +----------+  +----------+  +----------+               |
|                                                          |
+----------------------------------------------------------+
```

---

## Écrans restants à réaliser

| Écran | Priorité | Release |
|-------|----------|---------|
| Profil public d'un créateur (bio, prestations, albums, avis) | Haute | v0.1 |
| Gestion des albums (créateur) | Haute | v0.1 |
| Réservation d'un créneau (client) | Haute | v0.2 |
| Gestion des prestations et de l'agenda (créateur) | Haute | v0.2 |
| Tableau de bord créateur (demandes reçues, accepter / refuser) | Haute | v0.2 |
| Tableau de bord client (mes réservations, suivi des statuts) | Haute | v0.2 |
| Réservation multiple — pack | Moyenne | v0.3 |
| Avis et notes (formulaire client) | Moyenne | v0.3 |
| Back-office administrateur | Moyenne | v1.0 |

---

## Navigation par rôle

| Écran | Visiteur | Client | Créateur | Admin |
|-------|----------|--------|----------|-------|
| Galerie publique | ✓ | ✓ | ✓ | ✓ |
| Connexion / Inscription | ✓ | — | — | — |
| Création profil créateur | — | — | ✓ | — |
| Recherche | ✓ | ✓ | ✓ | ✓ |
| Tableau de bord | — | ✓ | ✓ | — |
| Back-office admin | — | — | — | ✓ |
