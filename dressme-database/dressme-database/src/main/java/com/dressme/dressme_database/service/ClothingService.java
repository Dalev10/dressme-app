package com.dressme.dressme_database.service;

import com.dressme.dressme_database.schema.dto.AuditUpdateRequest;
import com.dressme.dressme_database.schema.dto.CatalogDTO;
import com.dressme.dressme_database.schema.dto.ClothingCreateRequest;
import com.dressme.dressme_database.schema.dto.ClothingItemResponse;

import java.util.List;
import java.util.UUID;

public interface ClothingService {

    /**
     * Paso 1 — Registro inicial de la prenda.
     *
     * Persiste la prenda con imageUrl y is_processed = false.
     * La categoría queda NULL hasta que dressme-ai complete el análisis.
     *
     * @return la prenda creada con su UUID asignado.
     */
    ClothingItemResponse createClothing(ClothingCreateRequest request);

    /**
     * Paso 2 — Actualización post-análisis de Vision AI.
     *
     * Actualiza tbl_clothes (category_id, is_processed = true) y
     * persiste o actualiza tbl_clothing_ai_audit con la predicción completa.
     * Operación @Transactional: ambas tablas se actualizan en una sola unidad.
     *
     * @return la prenda con el estado actualizado.
     */
    ClothingItemResponse applyAiAudit(AuditUpdateRequest request);

    /**
     * Devuelve todas las prendas del guardarropa de un usuario,
     * ordenadas por fecha de creación descendente.
     */
    List<ClothingItemResponse> getWardrobeByUser(UUID userId);

    /**
     * Devuelve una prenda específica por su ID.
     * Lanza RuntimeException (→ 404) si no existe.
     */
    ClothingItemResponse getClothingById(UUID clothingId);

    /**
     * Elimina una prenda y sus registros dependientes (audit, colores, estilos…).
     * Lanza RuntimeException (→ 404) si no existe o no pertenece al usuario.
     */
    void deleteClothing(UUID clothingId, UUID userId);

    /**
     * Catálogo de referencia: categorías, ocasiones y climas activos.
     * Usado por el frontend para poblar filtros y dropdowns de corrección.
     */
    CatalogDTO getCatalog();
}