package com.dressme.dressme_back.schema.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload que envía el orquestador a dressme-database cuando termina
 * el análisis Vision de una prenda.
 *
 * Estructura APLANADA: los valores HSL del color detectado se envían como
 * campos individuales (detectedHue, detectedSaturation, detectedLightness)
 * en lugar de un objeto anidado, para coincidir con dressme-database.
 *
 * Paso 2 del flujo wardrobe: actualizar tbl_clothes + tbl_clothing_ai_audit.
 *
 * Todos los campos de predicción son obligatorios para garantizar que
 * el motor de recomendaciones tenga datos completos con qué trabajar.
 */
public record AuditUpdateRequest(

    /** ID de la prenda a actualizar */
    @NotNull(message = "El clothingId es obligatorio")
    UUID clothingId,

    /** Categoría predicha por Vision (debe existir en tbl_clothing_categories) */
    @NotNull(message = "La categoría predicha es obligatoria")
    UUID predictedCategoryId,

    /** Estilo predicho (debe existir en tbl_styles) */
    @NotNull(message = "El estilo predicho es obligatorio")
    UUID predictedStyleId,

    /** Color dominante predicho (debe existir en tbl_colors) */
    @NotNull(message = "El color predicho es obligatorio")
    UUID predictedColorId,

    /** Hue detectado por Gemini Vision [0-360] */
    @NotNull(message = "El hue detectado es obligatorio")
    Integer detectedHue,

    /** Saturation detectado por Gemini Vision [0-100] */
    @NotNull(message = "El saturation detectado es obligatorio")
    Integer detectedSaturation,

    /** Lightness detectado por Gemini Vision [0-100] */
    @NotNull(message = "El lightness detectado es obligatorio")
    Integer detectedLightness,

    /** Condición climática predicha (debe existir en tbl_weather) */
    @NotNull(message = "El clima predicho es obligatorio")
    UUID predictedWeatherId,

    /** Ocasión predicha (debe existir en tbl_occasions) */
    @NotNull(message = "La ocasión predicha es obligatoria")
    UUID predictedOccasionId,

    /** Score de confianza del modelo [0.00 – 1.00] */
    @NotNull
    @DecimalMin(value = "0.00", message = "El confidence score debe ser >= 0")
    @DecimalMax(value = "1.00", message = "El confidence score debe ser <= 1")
    BigDecimal confidenceScore,

    /**
     * Nombre del proveedor IA que realizó el análisis.
     * Valores esperados: "gemini_vision".
     */
    @NotBlank(message = "El aiProvider es obligatorio")
    String aiProvider

) {}