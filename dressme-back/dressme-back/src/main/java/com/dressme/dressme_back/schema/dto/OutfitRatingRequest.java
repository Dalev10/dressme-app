package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OutfitRatingRequest(

        @NotNull(message = "El rating es obligatorio")
        @Min(value = 1, message = "El rating mínimo es 1")
        @Max(value = 5, message = "El rating máximo es 5")
        @JsonProperty("rating")
        Integer rating

) {}