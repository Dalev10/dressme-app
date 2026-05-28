package com.dressme.dressme_back.schema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload enriquecido para GET /wardrobe/{clothingId}.
 *
 * Combina tbl_clothes + tbl_clothing_ai_audit + todas sus referencias
 * en una sola respuesta. El frontend lo usa para la vista de detalle
 * de una prenda y para pre-poblar el formulario de edición manual.
 *
 * Contraste con ClothingItemResponse (para el listado general),
 * que solo expone id, imageUrl, categoryName e isProcessed
 * para mantener el payload del list ligero.
 *
 * Todos los campos del audit son nullable:
 *   - null mientras isProcessed=false (análisis IA en curso).
 *   - populated cuando isProcessed=true.
 */
public record ClothingDetailResponse(

    // ── Datos de la prenda ────────────────────────────────────────────────────
    UUID id,
    UUID userId,
    String imageUrl,
    LocalDateTime createdAt,
    boolean isProcessed,

    // ── Categoría (de tbl_clothes) ────────────────────────────────────────────
    UUID categoryId,
    String categoryName,

    // ── Datos del audit IA (de tbl_clothing_ai_audit) ─────────────────────────
    /** null si la IA aún no procesó la prenda */
    UUID styleId,
    String styleName,

    UUID colorId,
    String colorName,
    /** Hex aproximado calculado desde HSL del catálogo — útil para la UI */
    String colorHex,

    UUID weatherId,
    String weatherName,

    UUID occasionId,
    String occasionName,

    /** Score de confianza del modelo [0.00 – 1.00] */
    BigDecimal confidenceScore,

    /**
     * true si el usuario corrigió manualmente los valores de la IA.
     * El scoring engine le da más peso a prendas corregidas por el usuario.
     */
    boolean wasCorrected,

    /** Proveedor que hizo el análisis: "google_vision", "clarifai" */
    String aiProvider

) {}
