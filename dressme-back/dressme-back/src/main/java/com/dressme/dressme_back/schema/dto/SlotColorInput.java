package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SlotColorInput(
    @NotNull Slot slot,
    @Min(0) @Max(360) int hue,
    @Min(0) @Max(100) int saturation,
    @Min(0) @Max(100) int lightness
) {
}
