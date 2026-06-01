-- =============================================================================
-- V1__initial_schema.sql
-- Schema inicial completo de DressMe
-- Generado a partir de los modelos JPA del módulo dressme-database
-- =============================================================================

-- Habilitar extensión pgvector (requerida para columnas vector(1536))
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================================================
-- TABLAS DE CATÁLOGO (sin dependencias externas)
-- =============================================================================

CREATE TABLE tbl_providers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    auth_endpoint   TEXT,
    icon_url        VARCHAR(255),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tbl_colors (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    hue         INTEGER      NOT NULL,
    saturation  INTEGER      NOT NULL,
    lightness   INTEGER      NOT NULL,
    is_neutral  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE tbl_occasions (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tbl_weather (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tbl_styles (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tbl_dress_codes (
    id               UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(150) NOT NULL UNIQUE,
    description      TEXT         NOT NULL,
    embedding_vector vector(1536) NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Categorías de ropa con jerarquía (parent_id autorreferenciada)
CREATE TABLE tbl_clothing_categories (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    parent_id   UUID         REFERENCES tbl_clothing_categories(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- STYLE CARDS (onboarding)
-- =============================================================================

CREATE TABLE tbl_style_cards (
    id                   UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(100) NOT NULL UNIQUE,
    semantic_description TEXT         NOT NULL,
    image_url            TEXT,
    tags                 TEXT,
    embedding_vector     vector(1536),          -- NULL hasta que seed_embeddings.py lo rellene
    display_order        INTEGER      NOT NULL DEFAULT 0,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tabla puente StyleCard ↔ Style (micro-estilos de la tarjeta)
CREATE TABLE tbl_style_card_mappings (
    style_card_id   UUID NOT NULL REFERENCES tbl_style_cards(id)          ON DELETE CASCADE,
    style_id        UUID NOT NULL REFERENCES tbl_styles(id)                ON DELETE CASCADE,
    PRIMARY KEY (style_card_id, style_id)
);

-- =============================================================================
-- COMPATIBILIDAD DE COLORES
-- =============================================================================

CREATE TABLE tbl_color_compatibility (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    color_a_id          UUID             NOT NULL REFERENCES tbl_colors(id),
    color_b_id          UUID             NOT NULL REFERENCES tbl_colors(id),
    compatibility_score DOUBLE PRECISION NOT NULL,
    created_at          TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- USUARIOS E IDENTIDADES
-- =============================================================================

CREATE TABLE tbl_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(255),
    profile_picture VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE tbl_user_identities (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES tbl_users(id)     ON DELETE CASCADE,
    provider_id      UUID         NOT NULL REFERENCES tbl_providers(id),
    provider_user_id VARCHAR(255) NOT NULL UNIQUE,
    access_token     TEXT,
    refresh_token    TEXT,
    expires_at       TIMESTAMP,
    scopes           TEXT,
    updated_at       TIMESTAMP
);

-- Perfil de gustos del usuario (taste vector para el motor de recomendación)
CREATE TABLE tbl_user_taste_profile (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL UNIQUE REFERENCES tbl_users(id) ON DELETE CASCADE,
    taste_vector vector(1536) NOT NULL,
    source_type  VARCHAR(50) NOT NULL,       -- 'pinterest', 'google_onboarding', 'manual_game'
    is_calibrated BOOLEAN    NOT NULL DEFAULT FALSE,
    last_updated TIMESTAMP
);

-- Selecciones del onboarding (LIKE/DISLIKE/SKIP por tarjeta)
CREATE TABLE tbl_onboarding_selections (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES tbl_users(id)       ON DELETE CASCADE,
    style_card_id UUID        NOT NULL REFERENCES tbl_style_cards(id),
    reaction      VARCHAR(10) NOT NULL CHECK (reaction IN ('LIKE', 'DISLIKE', 'SKIP')),
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_style_card UNIQUE (user_id, style_card_id)
);

-- =============================================================================
-- GUARDARROPA (prendas)
-- =============================================================================

-- NOTA: embedding_vector es TEXT aquí (estado inicial antes de V3).
-- V3 lo migrará a vector(1536).
CREATE TABLE tbl_clothes (
    id               UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID    NOT NULL REFERENCES tbl_users(id) ON DELETE CASCADE,
    category_id      UUID    REFERENCES tbl_clothing_categories(id),   -- nullable: se asigna tras análisis de Vision
    image_url        TEXT    NOT NULL,
    is_processed     BOOLEAN NOT NULL DEFAULT FALSE,
    embedding_vector TEXT,                                               -- JSON serializado; V3 lo convierte a vector(1536)
    is_embedding_stale BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Auditoría de la IA por prenda
CREATE TABLE tbl_clothing_ai_audit (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clothes_id            UUID UNIQUE NOT NULL REFERENCES tbl_clothes(id) ON DELETE CASCADE,
    confidence_score      NUMERIC(3, 2),
    predicted_category_id UUID         REFERENCES tbl_clothing_categories(id),
    predicted_style_id    UUID         REFERENCES tbl_styles(id),
    predicted_color_id    UUID         REFERENCES tbl_colors(id),
    detected_hue          INTEGER,
    detected_saturation   INTEGER,
    detected_lightness    INTEGER,
    predicted_weather_id  UUID         REFERENCES tbl_weather(id),
    predicted_occasion_id UUID         REFERENCES tbl_occasions(id),
    was_corrected         BOOLEAN      NOT NULL DEFAULT FALSE,
    ai_provider           VARCHAR(50),                                  -- 'google_vision', 'clarifai'
    updated_at            TIMESTAMP
);

-- Relaciones M:N de prendas con catálogos
CREATE TABLE tbl_clothes_styles (
    id         UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    clothes_id UUID NOT NULL REFERENCES tbl_clothes(id)  ON DELETE CASCADE,
    style_id   UUID NOT NULL REFERENCES tbl_styles(id)
);

CREATE TABLE tbl_clothes_occasions (
    id          UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    clothes_id  UUID NOT NULL REFERENCES tbl_clothes(id)   ON DELETE CASCADE,
    occasion_id UUID NOT NULL REFERENCES tbl_occasions(id)
);

CREATE TABLE tbl_clothes_weather (
    id         UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    clothes_id UUID NOT NULL REFERENCES tbl_clothes(id)  ON DELETE CASCADE,
    weather_id UUID NOT NULL REFERENCES tbl_weather(id)
);

CREATE TABLE tbl_clothes_colors (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    clothes_id UUID           NOT NULL REFERENCES tbl_clothes(id) ON DELETE CASCADE,
    color_id   UUID           NOT NULL REFERENCES tbl_colors(id),
    is_primary BOOLEAN        NOT NULL DEFAULT FALSE,
    percentage NUMERIC(5, 2)
);

-- =============================================================================
-- OUTFITS
-- =============================================================================

CREATE TABLE tbl_outfits (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES tbl_users(id) ON DELETE CASCADE,
    dress_code_id  UUID         REFERENCES tbl_dress_codes(id),
    occasion_id    UUID         REFERENCES tbl_occasions(id),
    weather_id     UUID         REFERENCES tbl_weather(id),
    name           VARCHAR(100),
    rating         INTEGER,
    is_ai_processed BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Prendas que componen un outfit (M:N)
CREATE TABLE tbl_clothes_outfits (
    id         UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    clothes_id UUID NOT NULL REFERENCES tbl_clothes(id)  ON DELETE CASCADE,
    outfit_id  UUID NOT NULL REFERENCES tbl_outfits(id)  ON DELETE CASCADE
);

-- Auditoría IA del outfit (match_score, affinity_score, outfit_vector)
CREATE TABLE tbl_outfit_ai_audit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outfit_id       UUID         UNIQUE NOT NULL REFERENCES tbl_outfits(id) ON DELETE CASCADE,
    match_score     NUMERIC(3, 2),
    affinity_score  NUMERIC(3, 2),
    outfit_vector   vector(1536),
    ai_feedback_log TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- ÍNDICES DE RENDIMIENTO
-- =============================================================================

-- Búsquedas frecuentes por usuario
CREATE INDEX idx_clothes_user_id       ON tbl_clothes(user_id);
CREATE INDEX idx_outfits_user_id       ON tbl_outfits(user_id);
CREATE INDEX idx_user_identities_user  ON tbl_user_identities(user_id);
CREATE INDEX idx_onboarding_user       ON tbl_onboarding_selections(user_id);

-- Búsqueda de identidad por provider + provider_user_id (login OAuth)
CREATE INDEX idx_user_identity_provider ON tbl_user_identities(provider_id, provider_user_id);

-- Búsqueda de outfits sin audit (procesamiento asíncrono)
CREATE INDEX idx_outfits_not_processed ON tbl_outfits(id) WHERE is_ai_processed = FALSE;

-- Búsqueda de prendas sin embedding (vectorización pendiente)
CREATE INDEX idx_clothes_no_embedding  ON tbl_clothes(id) WHERE embedding_vector IS NULL;