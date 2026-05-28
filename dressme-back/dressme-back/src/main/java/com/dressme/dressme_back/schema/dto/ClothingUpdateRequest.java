package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Payload para PATCH /wardrobe/{clothingId} — corrección manual de una prenda.
 *
 * El usuario puede corregir cualquier campo que la IA predijo incorrectamente.
 * Todos los campos son obligatorios: la UI siempre envía el estado completo
 * del formulario, no solo los campos modificados (simplifica la lógica de merge).
 *
 * Al persistir, ClothingServiceImpl marcará was_corrected = true en el audit,
 * señal que el scoring engine usa para dar mayor peso a esta prenda.
 */
public record ClothingUpdateRequest(

    @NotNull(message = "La categoría es obligatoria")
    UUID categoryId,

    @NotNull(message = "El estilo es obligatorio")
    UUID styleId,

    @NotNull(message = "El color es obligatorio")
    UUID colorId,

    @NotNull(message = "El clima es obligatorio")
    UUID weatherId,

    @NotNull(message = "La ocasión es obligatoria")
    UUID occasionId

) {}