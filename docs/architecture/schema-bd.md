# Schéma physique de la base de données

Projet **Clic & Clap** — version **v0.2** (en cours : prestations)

La base est une **PostgreSQL 17** unique, partagée par tous les modules du monolithe. Les tables sont créées au démarrage de l'application par [`schema.sql`](../../backend/src/main/resources/schema.sql) (`spring.sql.init.mode=always`, instructions `CREATE … IF NOT EXISTS`) et lues ou écrites avec `JdbcClient`, en SQL explicite et paramétré.

Chaque table appartient à **un seul module**. Seul ce module y accède directement ; les autres passent par son API publique.

> Ce schéma décrit exactement `schema.sql`. Il est mis à jour à chaque release qui ajoute ou modifie des tables.

---

## Diagramme

```mermaid
erDiagram
    users ||--o| creator_profiles : "a un profil"
    creator_profiles ||--o{ albums : "possède"
    albums ||--o{ media : "contient"
    creator_profiles ||--o{ creator_categories : "propose"
    categories ||--o{ creator_categories : "classe"
    creator_profiles ||--o{ offerings : "propose"
    offerings ||--o{ offering_options : "a pour options"
    categories |o--o{ offerings : "classe"

    users {
        BIGSERIAL id PK
        VARCHAR(255) email UK "NOT NULL"
        VARCHAR(255) sub UK "NOT NULL, identifiant Zitadel"
        VARCHAR(20) role "NOT NULL, CLIENT | CREATOR | ADMIN"
        BOOLEAN suspended "NOT NULL, défaut FALSE"
        TIMESTAMPTZ created_at "NOT NULL, défaut now()"
    }

    categories {
        SERIAL id PK
        VARCHAR(100) name UK "NOT NULL"
    }

    creator_profiles {
        BIGINT user_id PK, FK "→ users.id"
        VARCHAR(100) display_name "NOT NULL"
        VARCHAR(500) photo_url
        TEXT bio
        VARCHAR(100) city "indexé"
        VARCHAR(100) country "indexé"
        VARCHAR(500) service_areas
        VARCHAR(20) validation_status "NOT NULL, PENDING | VALIDATED | REJECTED"
        VARCHAR(500) rejection_reason
    }

    creator_categories {
        BIGINT creator_id PK, FK "→ creator_profiles.user_id"
        INTEGER category_id PK, FK "→ categories.id"
    }

    albums {
        BIGSERIAL id PK
        BIGINT creator_id FK "→ creator_profiles.user_id, indexé"
        VARCHAR(100) name "NOT NULL"
        TIMESTAMPTZ created_at "NOT NULL, défaut now()"
    }

    media {
        BIGSERIAL id PK
        BIGINT album_id FK "→ albums.id, indexé"
        VARCHAR(500) url "NOT NULL"
        VARCHAR(255) cloudinary_public_id "NOT NULL"
        VARCHAR(10) media_type "NOT NULL, PHOTO | VIDEO"
        TIMESTAMPTZ created_at "NOT NULL, défaut now()"
    }

    offerings {
        BIGSERIAL id PK
        BIGINT creator_id FK "→ creator_profiles.user_id, indexé"
        VARCHAR(100) name "NOT NULL"
        INTEGER duration_minutes "NOT NULL, > 0"
        NUMERIC base_price "NOT NULL, (10,2), >= 0"
        INTEGER category_id FK "→ categories.id, facultatif"
        TIMESTAMPTZ created_at "NOT NULL, défaut now()"
    }

    offering_options {
        BIGSERIAL id PK
        BIGINT offering_id FK "→ offerings.id, indexé"
        VARCHAR(100) name "NOT NULL"
        NUMERIC extra_price "NOT NULL, (10,2), >= 0"
    }
```

**Légende** : `PK` clé primaire, `FK` clé étrangère, `UK` contrainte d'unicité. `||--o|` : un vers zéro ou un ; `||--o{` : un vers zéro ou plusieurs.

---

## Tables

### `users` — module `users`

Un compte de la plateforme, quel que soit son rôle.

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `BIGSERIAL` | clé primaire |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` |
| `sub` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` |
| `role` | `VARCHAR(20)` | `NOT NULL`, `CHECK (role IN ('CLIENT', 'CREATOR', 'ADMIN'))` |
| `suspended` | `BOOLEAN` | `NOT NULL`, défaut `FALSE` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`, défaut `now()` |

> L'authentification est déléguée à Zitadel : aucun mot de passe n'est stocké dans notre base. Un compte est identifié par `sub`, l'identifiant du compte chez Zitadel (claim `sub` du token). Les autres modules obtiennent l'`id` d'un utilisateur à partir de son `sub` avec `UserApi.findIdBySub`.

### `categories` — module `catalog`

Les types de prestation utilisés par les filtres de la galerie (mariage, sport, clip…).

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `SERIAL` | clé primaire |
| `name` | `VARCHAR(100)` | `NOT NULL`, `UNIQUE` |

### `creator_profiles` — module `creators`

Le profil d'un utilisateur au rôle `CREATOR`. La clé primaire est aussi la clé étrangère vers `users` : un compte a au plus un profil créateur.

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `user_id` | `BIGINT` | clé primaire, `REFERENCES users (id) ON DELETE CASCADE` |
| `display_name` | `VARCHAR(100)` | `NOT NULL` |
| `photo_url` | `VARCHAR(500)` | — |
| `bio` | `TEXT` | — |
| `city` | `VARCHAR(100)` | index `idx_creator_profiles_city`, et index d'expression `idx_creator_profiles_city_normalisee` sur `lower(trim(city))` (filtre de la galerie) |
| `country` | `VARCHAR(100)` | index `idx_creator_profiles_country`, et index d'expression `idx_creator_profiles_country_normalisee` sur `lower(trim(country))` (filtre de la galerie) |
| `service_areas` | `VARCHAR(500)` | — |
| `validation_status` | `VARCHAR(20)` | `NOT NULL`, défaut `'VALIDATED'`, `CHECK (validation_status IN ('PENDING', 'VALIDATED', 'REJECTED'))` |
| `rejection_reason` | `VARCHAR(500)` | — |

> `validation_status` vaut `VALIDATED` par défaut en v0.1 (activation automatique). La validation par l'administrateur arrive en v0.2.

### `creator_categories` — module `creators`

Table d'association : les types de prestation proposés par chaque créateur (relation plusieurs-à-plusieurs).

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `creator_id` | `BIGINT` | `NOT NULL`, `REFERENCES creator_profiles (user_id) ON DELETE CASCADE` |
| `category_id` | `INTEGER` | `NOT NULL`, `REFERENCES categories (id) ON DELETE CASCADE` |

Clé primaire composée : `(creator_id, category_id)`.

### `albums` — module `creators`

Les albums photo d'un créateur.

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `BIGSERIAL` | clé primaire |
| `creator_id` | `BIGINT` | `NOT NULL`, `REFERENCES creator_profiles (user_id) ON DELETE CASCADE`, index `idx_albums_creator` |
| `name` | `VARCHAR(100)` | `NOT NULL` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`, défaut `now()` |

### `media` — module `creators`

Les photos (et plus tard les vidéos) d'un album. Le fichier est stocké sur Cloudinary ; la base garde son URL et son identifiant Cloudinary.

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `BIGSERIAL` | clé primaire |
| `album_id` | `BIGINT` | `NOT NULL`, `REFERENCES albums (id) ON DELETE CASCADE`, index `idx_media_album` |
| `url` | `VARCHAR(500)` | `NOT NULL` |
| `cloudinary_public_id` | `VARCHAR(255)` | `NOT NULL` |
| `media_type` | `VARCHAR(10)` | `NOT NULL`, défaut `'PHOTO'`, `CHECK (media_type IN ('PHOTO', 'VIDEO'))` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`, défaut `now()` |

### `offerings` — module `offerings`

Les prestations à prix fixe d'un créateur (#27) : nom, durée et prix de base, affichées sur son profil public.

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `BIGSERIAL` | clé primaire |
| `creator_id` | `BIGINT` | `NOT NULL`, `REFERENCES creator_profiles (user_id) ON DELETE CASCADE`, index `idx_offerings_creator` |
| `name` | `VARCHAR(100)` | `NOT NULL` |
| `duration_minutes` | `INTEGER` | `NOT NULL`, `CHECK (duration_minutes > 0)` |
| `base_price` | `NUMERIC(10,2)` | `NOT NULL`, `CHECK (base_price >= 0)`, en euros |
| `category_id` | `INTEGER` | facultatif, `REFERENCES categories (id) ON DELETE SET NULL` (#123, ajoutée par `ALTER TABLE … ADD COLUMN IF NOT EXISTS`) |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`, défaut `now()` |

### `offering_options` — module `offerings`

Les options payantes d'une prestation (ex. drone, retouches avancées), chacune avec son supplément.

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `BIGSERIAL` | clé primaire |
| `offering_id` | `BIGINT` | `NOT NULL`, `REFERENCES offerings (id) ON DELETE CASCADE`, index `idx_offering_options_offering` |
| `name` | `VARCHAR(100)` | `NOT NULL` |
| `extra_price` | `NUMERIC(10,2)` | `NOT NULL`, `CHECK (extra_price >= 0)`, en euros |

### Suppressions en cascade

Supprimer un compte supprime son profil créateur, puis ses albums, puis leurs médias, ainsi que ses associations de catégories et ses prestations avec leurs options. Supprimer une catégorie supprime seulement ses associations avec les créateurs, et retire la catégorie des prestations qui l'utilisaient.

---

## Données de départ

[`data.sql`](../../backend/src/main/resources/data.sql) insère 6 catégories au démarrage (`ON CONFLICT (name) DO NOTHING`, donc sans doublon) : Mariage, Événementiel, Sport, Produit, Clip, Portrait.

---

## Tables prévues dans les prochaines releases

Elles seront ajoutées à `schema.sql`, puis à ce document, au fil des releases :

| Release | Module | Données |
|---------|--------|---------|
| v0.2 | `offerings` | Créneaux disponibles, préavis et marge (#28) |
| v0.2 | `bookings` | Réservations et journal des changements de statut |
| v0.3 | `bookings` | Réservations multiples (packs) |
| v0.3 | `reputation` | Avis et notes |
| v0.3 – v1.0 | `moderation` | Signalements d'avis et de médias |
