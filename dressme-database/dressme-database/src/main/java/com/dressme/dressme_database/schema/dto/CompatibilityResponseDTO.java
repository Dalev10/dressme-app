package com.dressme.dressme_database.schema.dto;

public record CompatibilityResponseDTO(
    ColorResponseDTO colorA,
    ColorResponseDTO colorB,
    double compatibilityScore
){}
