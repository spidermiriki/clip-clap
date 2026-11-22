-- Schéma v0.1 (MVP) : comptes, profils créateurs, catégories, albums et médias.
-- Les tables des versions suivantes (prestations, réservations, avis, signalements)
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

-- Module bookings
-- Statuts possibles : EN_ATTENTE, CONFIRMEE, REFUSEE, ANNULEE, EXPIREE, TERMINEE (#39)
CREATE TABLE IF NOT EXISTS bookings (
    id          BIGSERIAL   PRIMARY KEY,
    client_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    creator_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Prestation et créneau réservés (#29, contrat sur #29 avec @spidermiriki) : pas encore NOT NULL
    -- ni REFERENCES, en attente de offerings (#119) et availability_slots (#128) ; passeront en
    -- NOT NULL REFERENCES quand #29 posera la création d'une réservation.
    offering_id BIGINT,
    slot_id     BIGINT,
    -- Prix total figé au moment de la réservation (#29) : une prestation modifiée ensuite ne change
    -- pas le prix d'une réservation existante.
    total_price NUMERIC(10, 2),
    status      VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
                CHECK (status IN ('EN_ATTENTE', 'CONFIRMEE', 'REFUSEE', 'ANNULEE', 'EXPIREE', 'TERMINEE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_bookings_client  ON bookings (client_id);
CREATE INDEX IF NOT EXISTS idx_bookings_creator ON bookings (creator_id);
-- Anti-collision (#30, scénario 21) : un créneau ne peut être confirmé que par une seule réservation
CREATE UNIQUE INDEX IF NOT EXISTS uq_bookings_slot_confirmee ON bookings (slot_id) WHERE status = 'CONFIRMEE';

-- Journal des changements de statut (#39) : une ligne par transition réussie, jamais modifiée
CREATE TABLE IF NOT EXISTS booking_status_history (
    id              BIGSERIAL   PRIMARY KEY,
    booking_id      BIGINT      NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    actor_id        BIGINT      NOT NULL REFERENCES users (id),
    -- NULL pour la première entrée (création de la réservation)
    previous_status VARCHAR(20) CHECK (previous_status IN ('EN_ATTENTE', 'CONFIRMEE', 'REFUSEE', 'ANNULEE', 'EXPIREE', 'TERMINEE')),
    new_status      VARCHAR(20) NOT NULL CHECK (new_status IN ('EN_ATTENTE', 'CONFIRMEE', 'REFUSEE', 'ANNULEE', 'EXPIREE', 'TERMINEE')),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_booking_status_history_booking ON booking_status_history (booking_id);
