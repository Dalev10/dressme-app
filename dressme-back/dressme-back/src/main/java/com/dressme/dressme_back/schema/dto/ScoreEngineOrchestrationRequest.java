package com.dressme.dressme_back.schema.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ScoreEngineOrchestrationRequest(
    @NotNull UUID userId,
    @NotNull UUID outfitId,
    @NotNull @Valid ColorScoreRequest color,
    @DecimalMin("0.0") @DecimalMax("1.0") double tasteScore,
    List<List<Float>> outfitEmbeddings
) {}