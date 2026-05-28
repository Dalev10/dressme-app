package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Payload que llega del Gateway cuando el usuario corrige manualmente
 * los campos de una prenda.
 *
 * Idéntico en estructura a ClothingUpdateRequest de dressme-database,
 * pero vive en dressme-back para desacoplar los contratos de cada servicio.
 * El orquestador lo transforma a ClothingUpdateRequest antes de llamar
 * a dressme-database.
 */
public record ClothingEditRequest(

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