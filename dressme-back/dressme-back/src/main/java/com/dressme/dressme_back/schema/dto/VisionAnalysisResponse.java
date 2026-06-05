// ─── VisionAnalysisResponse.java ─────────────────────────────────────────────
// Respuesta que devuelve dressme-ai después de analizar la imagen con Vision
// y mapear los labels al catálogo mediante Gemini.
//
// Este DTO es el contrato entre dressme-back y dressme-ai.
// dressme-back lo recibe y lo transforma en AuditUpdateRequest
// para persistir en dressme-database.
//
// ESTRUCTURA APLANADA: Los valores HSL del color se envían como campos
// individuales (detectedHue, detectedSaturation, detectedLightness) en lugar
// de un objeto anidado, para homogeneidad del pipeline completo.

package com.dressme.dressme_back.schema.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VisionAnalysisResponse(

    /** ID de la prenda que fue analizada (echo del request) */
    UUID clothingId,

    /** Categoría resuelta por Gemini a partir de los labels de Vision */
    UUID predictedCategoryId,

    /** Estilo resuelta por Gemini */
    UUID predictedStyleId,

    /** Color dominante predicho (debe existir en tbl_colors) */
    UUID predictedColorId,

    /** Hue detectado por Gemini Vision [0-360] */
    Integer detectedHue,

    /** Saturation detectado por Gemini Vision [0-100] */
    Integer detectedSaturation,

    /** Lightness detectado por Gemini Vision [0-100] */
    Integer detectedLightness,

    /** Clima predicho por Gemini */
    UUID predictedWeatherId,

    /** Ocasión predicha por Gemini */
    UUID predictedOccasionId,

    /**
     * Confianza del modelo entre 0.00 y 1.00.
     * Se toma del score más alto de los labels de Vision
     * después del mapeo semántico con Gemini.
     */
    BigDecimal confidenceScore,

    /**
     * Proveedor que realizó el análisis.
     * Valor esperado: "gemini_vision".
     */
    String aiProvider

) {}