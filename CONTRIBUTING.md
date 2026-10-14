# Guide de contribution — Clic & Clap

Ce document décrit comment configurer l'environnement local et suivre le workflow Git du projet.

---

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java (Temurin) | 17 |
| Gradle | via `./gradlew` (wrapper inclus) |
| Node.js | 22 |
| Docker Desktop | 29+ |
| Git | 2.x |

---

## Mise en place locale

```bash
# 1. Cloner le dépôt
git clone https://github.com/spidermiriki/clip-clap.git
cd clip-clap

# 2. Créer le fichier .env à la racine (ne jamais commiter)
cp .env.example .env
# Renseigner : ZITADEL_ISSUER, CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, etc.

# 3. Lancer la base de données (Docker requis)
docker compose up -d

# 4. Lancer le backend
cd backend
./gradlew bootRun

# 5. Lancer le frontend (autre terminal)
cd frontend
npm install
npm run dev
```

Les tests du backend (Testcontainers) démarrent leur propre container PostgreSQL — Docker doit être lancé :

```bash
cd backend
./gradlew test
```

---

## Workflow Git (GitHub Flow)

1. **Partir de `main` à jour** : `git switch main && git pull`
2. **Créer une branche** liée à l'issue : `git switch -c <type>/<n°>-<description>`
3. **Committer** en Conventional Commits (voir ci-dessous)
4. **Ouvrir une PR** vers `main` avec `Closes #<n°>` dans la description
5. **L'autre membre relit** et approuve (ou commente)
6. **Merger** via "Squash and merge" ou "Merge commit" ; supprimer la branche

> Ne jamais pousser directement sur `main`.

---

## Nommage des branches

```
<type>/<n°-issue>-<description-courte>
```

| Type | Quand | Exemple |
|------|-------|---------|
| `feat` | Nouvelle fonctionnalité | `feat/10-zitadel-auth` |
| `fix` | Correction de bug | `fix/42-collision-creneaux` |
| `test` | Tests uniquement | `test/7-testcontainers` |
| `docs` | Documentation | `docs/3-github-flow` |
| `ci` | CI/CD, GitHub Actions | `ci/6-github-actions` |
| `chore` | Maintenance, config | `chore/18-donnees-de-depart` |

---

## Conventional Commits

```
<type>(<portée>): <message court au présent>

[corps optionnel]
```

Exemples :

```
feat(bookings): add anti-collision check on slot confirmation
fix(creators): prevent deleted creator from appearing in gallery
docs(architecture): update C4 level-1 with Zitadel
test(users): add JdbcUserRepositoryTest with Testcontainers
chore: add .env.example with required variables
```

- La **portée** est le nom du module concerné (`users`, `bookings`, etc.) ou absente pour les changements transversaux.
- Le message est en **anglais**, au présent, sans majuscule initiale ni point final.

---

## Checklist avant d'ouvrir une PR

- [ ] `./gradlew test` passe localement (Docker requis pour Testcontainers)
- [ ] Aucun secret (mot de passe, clé API, token) dans le code commité
- [ ] La description de la PR contient `Closes #<n°>`
- [ ] L'issue est déplacée dans la colonne "In Review" du GitHub Project
