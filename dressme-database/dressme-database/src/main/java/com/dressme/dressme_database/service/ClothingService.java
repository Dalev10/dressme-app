package com.dressme.dressme_database.service;

import com.dressme.dressme_database.schema.dto.*;

import java.util.List;
import java.util.UUID;

public interface ClothingService {

    /** Paso 1 — Registro inicial (imagen subida, isProcessed=false). */
    ClothingItemResponse createClothing(ClothingCreateRequest request);

    /** Paso 2 — Aplicar resultado del análisis Vision+Gemini. */
    ClothingItemResponse applyAiAudit(AuditUpdateRequest request);

    /**
     * Lista resumida del guardarropa — para la vista grid del frontend.
     * Devuelve ClothingItemResponse (ligero): id, imageUrl, category, isProcessed.
     */
    List<ClothingItemResponse> getWardrobeByUser(UUID userId);

    /**
     * Detalle completo de una prenda — para la vista de detalle y edición.
     * Devuelve ClothingDetailResponse: prenda + todos los campos del audit IA.
     * Lanza RuntimeException (→ 404) si no existe.
     */
    ClothingDetailResponse getClothingDetail(UUID clothingId);

    /**
     * Corrección manual de los campos predichos por la IA.
     * Actualiza tbl_clothes (category) + tbl_clothing_ai_audit (style, color,
     * weather, occasion) y marca was_corrected = true.
     * Lanza RuntimeException (→ 404) si la prenda no existe o no pertenece al usuario.
     *
     * @return el detalle actualizado para que el frontend refleje los cambios.
     */
    ClothingDetailResponse updateClothing(UUID clothingId, UUID userId, ClothingUpdateRequest request);

    /**
     * Elimina la prenda y su registro de auditoría IA.
     * La imagen en el volumen la elimina dressme-back (tiene acceso al StorageService).
     * Lanza RuntimeException (→ 404) si no existe o no pertenece al usuario.
     */
    void deleteClothing(UUID clothingId, UUID userId);

    /**
     * Catálogo completo: categorías + estilos + colores + ocasiones + climas.
     * Para dropdowns de corrección manual y filtros del guardarropa.
     */
    CatalogDTO getCatalog();

    WardrobeEditCatalogDTO getEditCatalog();

    /**
     * Guardarropa filtrado para generación de outfits.
     *
     * Devuelve solo las prendas del usuario que cumplan todos los criterios:
     *   - isProcessed = true
     *   - embeddingVector IS NOT NULL
     *   - isEmbeddingStale = false
     *   - asociadas a la occasionId y weatherId indicadas
     *
     * El método realiza dos pasos internamente:
     *   1. findForOutfitGeneration → metadatos escalares via proyección JPA
     *   2. findEmbeddingsByIds     → hidratación del embeddingVector desde Clothing
     *
     * Esto evita problemas de serialización del tipo vector(1536) en proyecciones
     * planas y mantiene el query principal ligero.
     *
     * Lanza ResponseStatusException(404) si el usuario no existe.
     * Devuelve lista vacía si no hay prendas que cumplan los criterios
     * (dressme-back debe manejar este caso y retornar un error apropiado al Gateway).
     *
     * @param userId     propietario del guardarropa
     * @param occasionId ocasión seleccionada por el usuario para el outfit
     * @param weatherId  clima seleccionado por el usuario para el outfit
     */
    List<ClothingEmbeddingDTO> getClothingForOutfit(UUID userId, UUID occasionId, UUID weatherId);
}