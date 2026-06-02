-- =============================================================================
-- V4__seed_dress_codes.sql
-- Seed de dress codes base para el motor de score.
--
-- Nota: el vector se genera de forma determinista a partir del texto descriptivo
-- para evitar filas degeneradas en cero mientras se mantiene estabilidad entre
-- despliegues. Cuando exista un backfill con embeddings reales, esta migración
-- puede sustituirse por datos semánticos precalculados.
-- =============================================================================

CREATE OR REPLACE FUNCTION seed_dress_code_vector(seed_text TEXT)
RETURNS vector(1536)
LANGUAGE SQL
IMMUTABLE
AS $$
    WITH bytes AS (
        SELECT
            idx,
            get_byte(decode(md5(seed_text), 'hex'), (idx - 1) % 16) AS byte_value
        FROM generate_series(1, 1536) AS idx
    )
    SELECT array_agg(((byte_value::real - 128.0) / 128.0)::real ORDER BY idx)::vector(1536)
    FROM bytes;
$$;

INSERT INTO tbl_dress_codes (id, name, description, embedding_vector, is_active)
VALUES
('22222222-0000-0000-0000-000000000001', 'Casual', 'Relaxed, everyday dressing with denim, tees, sneakers and easy layering.', seed_dress_code_vector('Casual|Relaxed, everyday dressing with denim, tees, sneakers and easy layering.'), true),
('22222222-0000-0000-0000-000000000002', 'Smart Casual', 'Balanced outfits that feel polished yet relaxed, suitable for informal meetings and dinners.', seed_dress_code_vector('Smart Casual|Balanced outfits that feel polished yet relaxed, suitable for informal meetings and dinners.'), true),
('22222222-0000-0000-0000-000000000003', 'Business Casual', 'Professional clothing with a softer edge: chinos, blazers, knits and clean shoes.', seed_dress_code_vector('Business Casual|Professional clothing with a softer edge: chinos, blazers, knits and clean shoes.'), true),
('22222222-0000-0000-0000-000000000004', 'Business Formal', 'Tailored, structured and conservative suiting for highly formal work settings.', seed_dress_code_vector('Business Formal|Tailored, structured and conservative suiting for highly formal work settings.'), true),
('22222222-0000-0000-0000-000000000005', 'Cocktail', 'Evening looks that are elegant, expressive and refined for social events.', seed_dress_code_vector('Cocktail|Evening looks that are elegant, expressive and refined for social events.'), true),
('22222222-0000-0000-0000-000000000006', 'Athletic', 'Performance-driven activewear with sporty silhouettes and technical fabrics.', seed_dress_code_vector('Athletic|Performance-driven activewear with sporty silhouettes and technical fabrics.'), true)
ON CONFLICT DO NOTHING;