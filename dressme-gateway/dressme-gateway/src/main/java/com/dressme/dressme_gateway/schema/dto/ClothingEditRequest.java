package com.dressme.dressme_gateway.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Payload que el usuario envía al Gateway cuando corrige manualmente
 * los campos de una prenda (PATCH /api/v1/wardrobe/{clothingId}).
 *
 * El Gateway lo proxea tal cual a dressme-back, que lo transforma
 * internamente antes de llamar a dressme-database.
 */
public record ClothingEditRequest(

    @NotNull(message = "El tipo es obligatorio")
    UUID typeId,

    @NotNull(message = "La categoría es obligatoria")
    UUID categoryId,

    @NotNull(message = "El estilo es obligatorio")
    UUID styleId

) {}
