package com.dressme.dressme_back.schema.dto;

import java.util.List;
import java.util.UUID;

/**
 * Representación mínima de una prenda para el flujo de outfit generation.
 *
 * Espejo del schema Python ClothingEmbeddingInfo en dressme-ai.
 * Contiene solo los campos que el motor de recomendación necesita:
 *   - slot: para agrupar prendas y generar combinaciones válidas
 *   - HSL: para el cálculo de color compatibility
 *   - embeddingVector: para calcular outfit_vector y dresscode similarity
 *
 * Se construye a partir de ClothingEmbeddingDTO (de dressme-database)
 * en WardrobeOrchestratorServiceImpl.getWardrobeForOutfit().
 *
 * Valores esperados para slot: "TOP", "BOTTOM", "OUTERWEAR", "FOOTWEAR", "ONEPIECE"
 */
public record ClothingEmbeddingInfo(

        UUID clothingId,

        /**
         * Slot lógico derivado de la categoría padre de la prenda.
         * dressme-ai lo usa para agrupar prendas por tipo y
         * construir combinaciones coherentes (ej: TOP + BOTTOM, o ONEPIECE solo).
         */
        String slot,

        /** Hue detectado por Gemini Vision [0–360] */
        Integer hue,

        /** Saturation detectado [0–100] */
        Integer saturation,

        /** Lightness detectado [0–100] */
        Integer lightness,

        /**
         * Vector semántico de 1536 dimensiones de la prenda.
         * dressme-ai lo usa para: calcular outfit_vector (promedio de candidatos)
         * y calcular dresscode_score (similitud coseno vs dresscode_embedding).
         */
        List<Float> embeddingVector

) {}