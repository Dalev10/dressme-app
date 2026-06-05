package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrendDatasetConfigRequest(

        @NotNull
        @Size(min = 1536, max = 1536, message = "avgVector debe tener exactamente 1536 dimensiones")
        float[] avgVector,

        @Min(value = 1, message = "imageCount debe ser al menos 1")
        int imageCount,

        @NotBlank
        String modelUsed,

        String description
) {}