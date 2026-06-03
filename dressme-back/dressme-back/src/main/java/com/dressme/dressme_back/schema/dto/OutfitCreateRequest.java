package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OutfitCreateRequest(

        @NotNull(message = "El userId es obligatorio")
        @JsonProperty("userId")
        UUID userId,

        @NotEmpty(message = "El outfit debe tener al menos una prenda")
        @JsonProperty("clothingIds")
        List<UUID> clothingIds,

        @JsonProperty("dressCodeId")
        UUID dressCodeId,

        @JsonProperty("occasionId")
        UUID occasionId,

        @JsonProperty("weatherId")
        UUID weatherId,

        @JsonProperty("name")
        String name,

        @NotNull
        @DecimalMin("0.00") @DecimalMax("1.00")
        @JsonProperty("matchScore")
        BigDecimal matchScore,

        @NotNull
        @DecimalMin("0.00") @DecimalMax("1.00")
        @JsonProperty("affinityScore")
        BigDecimal affinityScore,

        @JsonProperty("totalScore")
        Double totalScore,

        @JsonProperty("outfitVector")
        float[] outfitVector

) {}