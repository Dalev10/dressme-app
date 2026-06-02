-- =============================================================================
-- V3__migrate_clothing_embedding_to_vector.sql
-- Migración: tbl_clothes.embedding_vector TEXT → vector(1536)
-- =============================================================================
--
-- Problema que resuelve:
--   La columna era TEXT (JSON serializado como "[0.1, 0.2, ...]").
--   OutfitAiAudit.outfit_vector y UserTasteProfile.taste_vector usan vector(1536)
--   nativo de pgvector. La inconsistencia impedía usar el operador <=> (coseno)
--   directamente en SQL para comparar prendas contra el gusto del usuario.
--
-- Estrategia de migración:
--   1. Agregar columna nueva embedding_vector_new vector(1536) nullable.
--   2. Migrar los datos existentes casteando TEXT → vector.
--      Las filas con NULL o valor vacío quedan NULL (sin datos que migrar en MVP).
--   3. Eliminar la columna antigua.
--   4. Renombrar la nueva.
--
-- Seguridad:
--   - La migración es no-destructiva: los datos existentes se preservan.
--   - En MVP todas las filas tienen embedding_vector = NULL (aún no vectorizado),
--     por lo que los pasos 2 y 3 son seguros sin pérdida de datos.
--   - Si hubiera datos TEXT válidos en producción, el CAST los convierte
--     automáticamente siempre que el formato sea "[f1,f2,...,f1536]".
-- =============================================================================

-- Verificar que pgvector está habilitado (falla explícitamente si no)
CREATE EXTENSION IF NOT EXISTS vector;

-- Paso 1: Agregar columna temporal con el tipo correcto
ALTER TABLE tbl_clothes
    ADD COLUMN embedding_vector_new vector(1536);

-- Paso 2: Migrar datos existentes (TEXT → vector)
-- Solo afecta filas con valor no nulo y no vacío.
-- En MVP todas son NULL, pero el script es correcto para producción también.
UPDATE tbl_clothes
SET embedding_vector_new = embedding_vector::vector
WHERE embedding_vector IS NOT NULL
  AND embedding_vector <> '';

-- Paso 3: Eliminar columna antigua
ALTER TABLE tbl_clothes
    DROP COLUMN embedding_vector;

-- Paso 4: Renombrar la nueva columna al nombre definitivo
ALTER TABLE tbl_clothes
    RENAME COLUMN embedding_vector_new TO embedding_vector;

-- Paso 5: Crear índice HNSW para búsqueda de similitud coseno eficiente.
-- El motor de recomendación usará: ORDER BY embedding_vector <=> taste_vector LIMIT N
-- HNSW es más rápido que IVFFlat para consultas con guardarropas pequeños (<10k prendas).
-- m=16, ef_construction=64: valores por defecto, adecuados para el MVP.
CREATE INDEX IF NOT EXISTS idx_clothes_embedding_hnsw
    ON tbl_clothes
    USING hnsw (embedding_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Verificación final
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tbl_clothes'
          AND column_name = 'embedding_vector'
          AND data_type = 'USER-DEFINED'  -- pgvector usa USER-DEFINED como data_type
    ) THEN
        RAISE EXCEPTION
            'Migración fallida: embedding_vector no existe o no es de tipo vector en tbl_clothes';
    END IF;
    RAISE NOTICE 'Migración V3 completada: tbl_clothes.embedding_vector es ahora vector(1536)';
END $$;