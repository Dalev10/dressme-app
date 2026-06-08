package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Payload para PATCH /wardrobe/{clothingId} — corrección manual de una prenda.
 *
 * El usuario puede corregir cualquier campo que la IA predijo incorrectamente.
 * typeId, categoryId y styleId son obligatorios. Los campos colorId, occasionIds
 * y weatherIds son opcionales: null significa "no cambiar este campo".
 *
 * Al persistir, ClothingServiceImpl marcará was_corrected = true en el audit,
 * señal que el scoring engine usa para dar mayor peso a esta prenda.
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