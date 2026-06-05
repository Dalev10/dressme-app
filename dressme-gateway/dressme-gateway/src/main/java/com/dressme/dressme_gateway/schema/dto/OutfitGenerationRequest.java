package com.dressme.dressme_gateway.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Payload que el usuario envía al Gateway para generar outfits
 * (POST /api/v1/outfits/generate).
 *
 * El Gateway extrae el userId del JWT y lo agrega como X-User-Id header
 * antes de proxear este body a dressme-back.
 *
 * dressCodeId es nullable: si el usuario no restringe por dress code,
 * el motor de scoring redistribuye ese peso entre los demás componentes.
 */
public record OutfitGenerationRequest(

        @NotNull(message = "La ocasión es obligatoria")
        UUID occasionId,

        @NotNull(message = "El clima es obligatorio")
        UUID weatherId,

        UUID dressCodeId

) {}
