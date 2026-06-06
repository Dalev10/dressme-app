package com.dressme.dressme_back.schema.dto;

import java.util.List;

/**
 * Respuesta del flujo de outfit generation.
 *
 * Devuelta por InternalOutfitController al Gateway tras completar
 * el ciclo completo: wardrobe → dressme-ai → scoring → persistencia.
 *
 * Los outfits vienen ordenados por totalScore descendente.
 * El Gateway proxea esta respuesta directamente al cliente.
 */
public record OutfitGenerationResponse(

        /**
         * Outfits generados, ordenados por totalScore descendente.
         * Cada OutfitResponse incluye los IDs de prendas, scores,
         * nombre sugerido, ocasión, clima y estado de procesamiento.
         */
        List<OutfitResponse> outfits,

        /**
         * Total de outfits persistidos en esta sesión de generación
         * Útil para que el cliente sepa cuántos outfits nuevos están disponibles
         */
        int totalGenerated,
        /**
         * Avisos no fatales ocurridos durante la generación.
         * Ejemplo: algunas prendas no pudieron repararse y fueron excluidas.
         */
        List<GenerationWarning> warnings

) {
        public OutfitGenerationResponse(List<OutfitResponse> outfits, int totalGenerated) {
                this(outfits, totalGenerated, List.of());
        }
}