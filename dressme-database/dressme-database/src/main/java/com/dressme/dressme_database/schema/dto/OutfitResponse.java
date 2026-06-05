package com.dressme.dressme_database.schema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vista resumida de un outfit para el grid del frontend.
 * Incluye imageUrls de las prendas para renderizar la preview
 * sin necesidad de un GET adicional por outfit.
 */
public record OutfitResponse(

    UUID id,
    UUID userId,
    String name,

    /** URLs de imagen de las prendas — para la preview visual del outfit */
    List<String> clothingImageUrls,

    /** IDs de las prendas — para navegar al detalle de cada una */
    List<UUID> clothingIds,

    UUID occasionId,
    String occasionName,

    UUID weatherId,
    String weatherName,

    /** Score compuesto [0.00–1.00] — para ordenar el grid */
    BigDecimal matchScore,

    /** Afinidad con el taste_vector [0.00–1.00] */
    BigDecimal affinityScore,

    /** Score total final calculado por el ScoreEngine — valor de ranking definitivo */
    Double totalScore,

    /** Calificación del usuario [1–5] — null si aún no calificó */
    Integer rating,

    boolean isAiProcessed,
    LocalDateTime createdAt

) {}