package com.dressme.dressme_database.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO ligero para el flujo de generación de outfits.
 *
 * Contiene únicamente los campos que dressme-ai necesita para:
 *   1. Agrupar prendas por slot (TOP, BOTTOM, OUTERWEAR, FOOTWEAR, ONEPIECE).
 *   2. Calcular color compatibility (HSL).
 *   3. Calcular outfit_vector (promedio de embeddingVector).
 *
 * NO es ClothingDetailResponse — ese DTO es para la vista del usuario.
 * Este es el payload mínimo del pipeline de recomendación.
 *
 * El campo slot se deriva del nombre de la categoría padre en dressme-database
 * y se resuelve en la query JPQL antes de construir este DTO.
 */
public record ClothingEmbeddingDTO(

    UUID clothingId,

    /**
     * Slot lógico derivado de la categoría de la prenda.
     * Valores esperados: "TOP", "BOTTOM", "OUTERWEAR", "FOOTWEAR", "ONEPIECE".
     * dressme-ai lo usa para agrupar prendas y generar combinaciones válidas.
     */
    String slot,

    /** Hue detectado por Gemini Vision [0–360] */
    Integer hue,

    /** Saturation detectado [0–100] */
    Integer saturation,

    /** Lightness detectado [0–100] */
    Integer lightness,

    /**
     * Vector semántico de 1536 dimensiones.
     * Nunca null en este DTO: el query que lo genera filtra
     * embedding_vector IS NOT NULL + is_embedding_stale = false.
     */
    List<Float> embeddingVector,

    /** Nombre de la ocasión — para filtro secundario en dressme-ai */
    String occasion,

    /** Nombre del clima — para filtro secundario en dressme-ai */
    String weather

) {}