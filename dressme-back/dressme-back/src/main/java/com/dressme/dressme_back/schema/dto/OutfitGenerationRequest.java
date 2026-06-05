package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request de generación de outfits.
 *
 * Llega del Gateway al InternalOutfitController de dressme-back.
 * El userId no viene aquí — lo extrae el Gateway del JWT y lo
 * pasa como parámetro de path o header separado.
 *
 * dressCodeId es nullable: el usuario puede pedir outfits sin
 * restringir el dress code, en cuyo caso el ScoreEngine redistribuye
 * el peso del dresscode_score entre los demás componentes.
 */
public record OutfitGenerationRequest(

        @NotNull(message = "La ocasión es obligatoria")
        UUID occasionId,

        @NotNull(message = "El clima es obligatorio")
        UUID weatherId,

        /**
         * Dress code seleccionado por el usuario (opcional).
         * Si es null, dressme-ai devuelve dresscode_score = 0.5
         * y el ScoreEngine redistribuye ese peso.
         */
        UUID dressCodeId

) {}