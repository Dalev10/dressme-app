package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ColorCompatibilityRequestDTO(
    @Min(0)   @Max(360) int hue1,
    @Min(0)   @Max(100) int saturation1,
    @Min(0)   @Max(100) int lightness1,
    @Min(0)   @Max(360) int hue2,
    @Min(0)   @Max(100) int saturation2,
    @Min(0)   @Max(100) int lightness2
) {
}
