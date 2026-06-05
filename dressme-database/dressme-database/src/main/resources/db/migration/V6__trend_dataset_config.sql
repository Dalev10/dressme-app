CREATE TABLE tbl_trend_dataset_config (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    avg_vector   vector(1536) NOT NULL,
    image_count  INTEGER      NOT NULL,
    model_used   VARCHAR(100) NOT NULL,
    description  TEXT,
    computed_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
