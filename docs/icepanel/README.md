# IcePanel — fichier d'import

## Import dans IcePanel

1. Ouvrir IcePanel et créer un **nouveau landscape vide**.
2. Aller dans **Admin → Import**.
3. Sélectionner le fichier `clic-et-clap-icepanel.json` présent dans ce dossier.
4. Confirmer l'import.

## Ce que l'import crée

| Catégorie | Nombre | Détail |
|-----------|--------|--------|
| Objets | 22 | 4 acteurs, 5 conteneurs (dont 2 systèmes externes), 9 modules, 4 classes (niveau 4), groupes d'environnement |
| Connexions | 62 | Relations entre acteurs, conteneurs, modules et classes |

**Diagrammes inclus :**

| Niveau | Titre |
|--------|-------|
| 1 | Contexte |
| 2 | Conteneurs |
| 3 | Modules du monolithe |
| 3 | Flux de réservation (zoom `bookings`) |

## Statuts

- `future` (cercle violet pointillé) : **Brevo** (e-mails), **Google OAuth**, module **notifications** et leurs flèches — non prévus dans le périmètre actuel.
- `live` : tout le reste.

## Limitations connues

- Brevo et Google ne disposent pas de logo officiel dans IcePanel ; leurs boîtes restent sans icône.
- Le fichier JSON ne contient pas de captures d'écran ; celles-ci sont dans `docs/images/c4/`.
