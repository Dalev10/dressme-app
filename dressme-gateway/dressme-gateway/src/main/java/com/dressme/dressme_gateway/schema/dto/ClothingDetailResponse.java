package com.dressme.dressme_gateway.schema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload enriquecido para GET /api/v1/wardrobe/{clothingId}.
 *
 * Combina datos de la prenda + auditoría IA + todas sus referencias
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

    // ── Categoría ─────────────────────────────────────────────────────────────
    UUID categoryId,
    String categoryName,

    // ── Datos del audit IA ────────────────────────────────────────────────────
    /** null si la IA aún no procesó la prenda */
    UUID styleId,
    String styleName,

    UUID colorId,
    String colorName,
    /** Hex aproximado calculado desde HSL del catálogo — útil para la UI */
    String colorHex,

    /** Valores HSL exactos detectados por Gemini Vision */
    Integer detectedHue,
    Integer detectedSaturation,
    Integer detectedLightness,

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
