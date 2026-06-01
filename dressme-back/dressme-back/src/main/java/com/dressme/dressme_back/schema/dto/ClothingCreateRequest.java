// ─── ClothingCreateRequest.java ──────────────────────────────────────────────
// Paso 1 del flujo wardrobe: el usuario sube la imagen.
// La categoría llega NULA intencionalmente; Vision la asignará en el Paso 2.

package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClothingCreateRequest(

    @NotNull(message = "El userId es obligatorio")
    UUID userId,

    /**
     * URL de la imagen ya almacenada en el CDN / storage externo.
     * dressme-back sube la imagen antes de llamar a este endpoint
     * y nos pasa la URL resultante.
     */
    @NotBlank(message = "La URL de imagen es obligatoria")
    String imageUrl

) {}