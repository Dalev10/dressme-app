package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload para PATCH /internal/outfits/{outfitId}/rating.
 *
 * Escala Likert 1–5:
 *   1–2 → outfit penalizado en futuras generaciones
 *   3   → neutro
 *   4–5 → outfit priorizado como referencia de estilo del usuario
 */
public record OutfitRatingRequest(

    @NotNull(message = "El rating es obligatorio")
    @Min(value = 1, message = "El rating mínimo es 1")
    @Max(value = 5, message = "El rating máximo es 5")
    Integer rating

) {}