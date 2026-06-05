package com.dressme.dressme_gateway.schema.dto;

import java.util.List;

/**
 * Respuesta del flujo de outfit generation.
 * Espejo del OutfitGenerationResponse de dressme-back — el Gateway lo proxea sin transformación.
 *
 * Los outfits vienen ordenados por totalScore descendente.
 */
public record OutfitGenerationResponse(
        List<OutfitResponse> outfits,
        int totalGenerated
) {}
