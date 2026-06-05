package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.OutfitDetailResponse;
import com.dressme.dressme_back.schema.dto.OutfitGenerationRequest;
import com.dressme.dressme_back.schema.dto.OutfitGenerationResponse;
import com.dressme.dressme_back.schema.dto.OutfitRatingRequest;
import com.dressme.dressme_back.schema.dto.OutfitResponse;

import java.util.List;
import java.util.UUID;

public interface OutfitOrchestratorService {

    /**
     * Genera outfits para un usuario dado su armario, ocasión y clima.
     * Flujo: wardrobe → dressme-ai candidatos → scoring paralelo → top-N → persistir.
     */
    OutfitGenerationResponse generateOutfits(UUID userId, OutfitGenerationRequest request);

    /** Lista todos los outfits del usuario, ordenados por totalScore descendente. */
    List<OutfitResponse> getOutfits(UUID userId);

    /** Detalle completo de un outfit con slots, colores y scores del audit. */
    OutfitDetailResponse getOutfitDetail(UUID outfitId);

    /** Actualiza el rating (1–5) del usuario sobre un outfit. */
    OutfitResponse rateOutfit(UUID outfitId, UUID userId, OutfitRatingRequest request);
}
