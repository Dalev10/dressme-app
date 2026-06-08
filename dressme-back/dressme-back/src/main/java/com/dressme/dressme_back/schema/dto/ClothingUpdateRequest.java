package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Payload interno que el orquestador envía a dressme-database
 * para la corrección manual de una prenda.
 *
 * typeId, categoryId y styleId son obligatorios. Los campos colorId,
 * occasionIds y weatherIds son opcionales: null significa "no cambiar".
 */
public record ClothingUpdateRequest(

    @NotNull(message = "El tipo es obligatorio")
    UUID typeId,

    @NotNull(message = "La categoría es obligatoria")
    UUID categoryId,

    @NotNull(message = "El estilo es obligatorio")
    UUID styleId,

    // Optional — null means "do not change"
    UUID colorId,

    List<UUID> occasionIds,

    List<UUID> weatherIds

) {}