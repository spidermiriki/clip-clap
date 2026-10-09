-- Schéma v0.1 (MVP) : comptes, profils créateurs, catégories, albums et médias.
-- Les tables des versions suivantes (prestations, réservations, avis, signalements)
-- seront ajoutées au fil des releases.

-- Module users
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('CLIENT', 'CREATOR', 'ADMIN')),
    suspended     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

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
