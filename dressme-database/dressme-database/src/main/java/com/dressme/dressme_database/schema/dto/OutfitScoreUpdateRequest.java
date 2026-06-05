package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload para actualizar los scores de un outfit tras el cálculo del ScoreEngine.
 *
 * Se llama después de crear el outfit (POST /internal/outfits) una vez que
 * dressme-back ha calculado taste + total con el ScoreEngine.
 *
 * affinityScore → similitud con el taste_vector del usuario (tasteScore)
 * totalScore    → score compuesto final: color + dresscode + taste + trend
 */
public record OutfitScoreUpdateRequest(

    @NotNull
    @DecimalMin("0.00") @DecimalMax("1.00")
    BigDecimal affinityScore,

    @NotNull
    Double totalScore

) {}
