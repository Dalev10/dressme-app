package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload para PATCH /internal/outfits/{outfitId}/scores en dressme-database.
 * Se envía tras el cálculo del ScoreEngine en el orquestador.
 */
public record OutfitScoreUpdateRequest(

    @NotNull
    @DecimalMin("0.00") @DecimalMax("1.00")
    @JsonProperty("affinityScore")
    BigDecimal affinityScore,

    @NotNull
    @JsonProperty("totalScore")
    Double totalScore

) {}
