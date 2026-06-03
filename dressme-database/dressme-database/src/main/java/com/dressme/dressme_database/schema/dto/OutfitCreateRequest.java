package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OutfitCreateRequest(

    @NotNull(message = "El userId es obligatorio")
    UUID userId,

    @NotEmpty(message = "El outfit debe tener al menos una prenda")
    List<UUID> clothingIds,

    UUID dressCodeId,
    UUID occasionId,
    UUID weatherId,
    String name,

    /**
     * Score compuesto del ScoreEngine [0.00–1.00].
     * color (40%) + dresscode (20%) + taste (20%) + trend (20%).
     */
    @NotNull
    @DecimalMin("0.00") @DecimalMax("1.00")
    BigDecimal matchScore,

    /**
     * Similitud coseno entre outfit_vector y taste_vector [0.00–1.00].
     */
    @NotNull
    @DecimalMin("0.00") @DecimalMax("1.00")
    BigDecimal affinityScore,

    /**
     * Score total compuesto calculado por el ScoreEngine [0.0–1.0].
     * Pondera color + dresscode + taste + trend con redistribución de pesos.
     * Nullable: outfits legacy creados antes del ScoreEngine no tienen este valor.
     */
    Double totalScore,

    /**
     * Vector semántico del outfit (1536 dims).
     * Promedio ponderado de los embedding_vector de las prendas.
     * Nullable hasta que todas las prendas estén vectorizadas.
     */
    float[] outfitVector

) {}