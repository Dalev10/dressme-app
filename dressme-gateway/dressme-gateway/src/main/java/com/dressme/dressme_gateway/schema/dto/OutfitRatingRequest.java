package com.dressme.dressme_gateway.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload para calificar un outfit
 * (PATCH /api/v1/outfits/{id}/rating).
 */
public record OutfitRatingRequest(

        @NotNull(message = "El rating es obligatorio")
        @Min(value = 1, message = "El rating mínimo es 1")
        @Max(value = 5, message = "El rating máximo es 5")
        @JsonProperty("rating")
        Integer rating

) {}
