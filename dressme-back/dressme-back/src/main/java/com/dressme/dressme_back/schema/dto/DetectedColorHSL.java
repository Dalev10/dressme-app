package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Color dominante detectado por Gemini Vision en la prenda.
 *
 * hue/saturation/lightness → valores exactos para el scoring engine
 *                            (armonía cromática: complementarios, análogos, triádicos)
 * colorCatalogId          → UUID del color canónico más cercano del catálogo
 *                           (para la UI: chip de color, filtros del guardarropa)
 */
public record DetectedColorHSL(

    @NotNull(message = "El hue es obligatorio")
    @Min(value = 0, message = "El hue debe estar entre 0 y 360")
    @Max(value = 360, message = "El hue debe estar entre 0 y 360")
    Integer hue,

    @NotNull(message = "El saturation es obligatorio")
    @Min(value = 0, message = "El saturation debe estar entre 0 y 100")
    @Max(value = 100, message = "El saturation debe estar entre 0 y 100")
    Integer saturation,

    @NotNull(message = "El lightness es obligatorio")
    @Min(value = 0, message = "El lightness debe estar entre 0 y 100")
    @Max(value = 100, message = "El lightness debe estar entre 0 y 100")
    Integer lightness,

    @NotNull(message = "El colorCatalogId es obligatorio")
    UUID colorCatalogId

) {}
