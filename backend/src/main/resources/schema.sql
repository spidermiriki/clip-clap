-- Schéma : comptes, profils créateurs, catégories, albums et médias (v0.1), prestations (v0.2).
-- Les tables des versions suivantes (agenda, réservations, avis, signalements)
-- seront ajoutées au fil des releases.

-- Module users
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    -- Identifiant du compte chez Zitadel (claim sub du token) : aucun mot de passe n'est stocké
    sub           VARCHAR(255) NOT NULL UNIQUE,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('CLIENT', 'CREATOR', 'ADMIN')),
    suspended     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Index partiel : accélère findAllSuspendedIds (#98), les suspendus restant peu nombreux
CREATE INDEX IF NOT EXISTS idx_users_suspended ON users (id) WHERE suspended;

-- Module catalog
CREATE TABLE IF NOT EXISTS categories (
    id   SERIAL       PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Module creators
CREATE TABLE IF NOT EXISTS creator_profiles (
    user_id           BIGINT       PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    display_name      VARCHAR(100) NOT NULL,
    photo_url         VARCHAR(500),
    bio               TEXT,
    city              VARCHAR(100),
    country           VARCHAR(100),
    service_areas     VARCHAR(500),
    -- Validation automatique en v0.1, par l'administrateur à partir de la v0.2
    validation_status VARCHAR(20)  NOT NULL DEFAULT 'VALIDATED'
                      CHECK (validation_status IN ('PENDING', 'VALIDATED', 'REJECTED')),
    rejection_reason  VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_creator_profiles_city    ON creator_profiles (city);
CREATE INDEX IF NOT EXISTS idx_creator_profiles_country ON creator_profiles (country);
-- Filtres de la galerie (#14) : même expression que la requête, sans quoi ces index ne servent pas
CREATE INDEX IF NOT EXISTS idx_creator_profiles_city_normalisee    ON creator_profiles (lower(trim(city)));
CREATE INDEX IF NOT EXISTS idx_creator_profiles_country_normalisee ON creator_profiles (lower(trim(country)));

CREATE TABLE IF NOT EXISTS creator_categories (
    creator_id  BIGINT  NOT NULL REFERENCES creator_profiles (user_id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    PRIMARY KEY (creator_id, category_id)
);

CREATE TABLE IF NOT EXISTS albums (
    id         BIGSERIAL    PRIMARY KEY,
    creator_id BIGINT       NOT NULL REFERENCES creator_profiles (user_id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_albums_creator ON albums (creator_id);

CREATE TABLE IF NOT EXISTS media (
    id                   BIGSERIAL    PRIMARY KEY,
    album_id             BIGINT       NOT NULL REFERENCES albums (id) ON DELETE CASCADE,
    url                  VARCHAR(500) NOT NULL,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    media_type           VARCHAR(10)  NOT NULL DEFAULT 'PHOTO' CHECK (media_type IN ('PHOTO', 'VIDEO')),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_media_album ON media (album_id);

-- Module offerings (v0.2)
-- Prestations à prix fixe d'un créateur (#27), avec leurs options payantes
CREATE TABLE IF NOT EXISTS offerings (
    id               BIGSERIAL     PRIMARY KEY,
    creator_id       BIGINT        NOT NULL REFERENCES creator_profiles (user_id) ON DELETE CASCADE,
    name             VARCHAR(100)  NOT NULL,
    duration_minutes INTEGER       NOT NULL CHECK (duration_minutes > 0),
    base_price       NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_offerings_creator ON offerings (creator_id);

-- Catégorie facultative d'une prestation (#123). ALTER plutôt qu'une colonne du CREATE : la table
-- peut déjà exister (déploiement de #27). Une catégorie inconnue est refusée par la clé étrangère.
ALTER TABLE offerings ADD COLUMN IF NOT EXISTS category_id INTEGER REFERENCES categories (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS offering_options (
    id          BIGSERIAL     PRIMARY KEY,
    offering_id BIGINT        NOT NULL REFERENCES offerings (id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    extra_price NUMERIC(10,2) NOT NULL CHECK (extra_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_offering_options_offering ON offering_options (offering_id);
