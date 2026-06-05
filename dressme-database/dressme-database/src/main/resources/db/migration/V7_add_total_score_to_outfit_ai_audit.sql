-- Agrega columna total_score al audit de outfits.
-- Nullable para compatibilidad con outfits generados antes del ScoreEngine.
ALTER TABLE tbl_outfit_ai_audit
    ADD COLUMN IF NOT EXISTS total_score DOUBLE PRECISION;

COMMENT ON COLUMN tbl_outfit_ai_audit.total_score
    IS 'Score compuesto final del ScoreEngine [0.0-1.0]. '
       'Pondera color + dresscode + taste + trend con redistribucion de pesos.';