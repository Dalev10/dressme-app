package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Payload que llega del Gateway cuando el usuario corrige manualmente
 * los campos de una prenda.
 *
 * Vive en dressme-back para desacoplar los contratos de cada servicio.
 * El orquestador lo transforma a ClothingUpdateRequest antes de llamar
 * a dressme-database.
 */
public record ClothingEditRequest(

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