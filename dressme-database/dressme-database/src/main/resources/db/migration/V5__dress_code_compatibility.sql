-- =============================================================================
-- V5__dress_code_compatibility.sql
-- Tabla de compatibilidad entre dress codes.
-- Cada fila (A, B) significa que un usuario que prefiere A puede usar
-- un outfit con dress code B con puntuación parcial (0.6).
-- La relación es dirigida: A compatible-con B no implica B compatible-con A.
-- =============================================================================

CREATE TABLE tbl_dress_code_compatibility (
    dress_code_id          UUID NOT NULL REFERENCES tbl_dress_codes(id) ON DELETE CASCADE,
    compatible_with_id     UUID NOT NULL REFERENCES tbl_dress_codes(id) ON DELETE CASCADE,
    PRIMARY KEY (dress_code_id, compatible_with_id)
);

CREATE INDEX idx_dress_code_compatibility_dc ON tbl_dress_code_compatibility(dress_code_id);

-- Compatibilidades iniciales (alineadas con la lógica previa en DressCodeScoreServiceImpl)
-- Casual → Smart Casual, Athletic
INSERT INTO tbl_dress_code_compatibility (dress_code_id, compatible_with_id) VALUES
('22222222-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000002'),
('22222222-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000006'),
-- Smart Casual → Casual, Business Casual
('22222222-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001'),
('22222222-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000003'),
-- Business Casual → Smart Casual, Business Formal
('22222222-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000002'),
('22222222-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000004'),
-- Business Formal → Business Casual
('22222222-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000003'),
-- Athletic → Casual
('22222222-0000-0000-0000-000000000006', '22222222-0000-0000-0000-000000000001')
-- Cocktail no tiene compatibles (ninguna fila)
ON CONFLICT DO NOTHING;
