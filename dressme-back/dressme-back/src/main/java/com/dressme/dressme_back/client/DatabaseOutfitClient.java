package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.OutfitCreateRequest;
import com.dressme.dressme_back.schema.dto.OutfitDetailResponse;
import com.dressme.dressme_back.schema.dto.OutfitRatingRequest;
import com.dressme.dressme_back.schema.dto.OutfitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feign client para operaciones de outfit contra dressme-database.
 *
 * Cubre el ciclo completo del flujo de outfit generation:
 *   - createOutfit: persiste cada candidato aprobado por el ScoreEngine
 *   - getOutfitsByUser: lista todos los outfits del usuario para el gateway
 *   - getOutfitById: detalle completo con slots y scores del audit
 *   - rateOutfit: actualiza el rating del usuario sobre un outfit
 */
@FeignClient(name = "dressme-database-outfit-client", url = "${app.services.database-url}")
public interface DatabaseOutfitClient {

    /**
     * Persiste un outfit generado por el pipeline AI + ScoreEngine.
     * Incluye clothingIds, scores (match, affinity, total) y outfitVector.
     */
    @PostMapping("/internal/outfits")
    OutfitResponse createOutfit(@RequestBody OutfitCreateRequest request);

    /**
     * Lista todos los outfits del usuario ordenados por totalScore descendente.
     */
    @GetMapping("/internal/outfits/user/{userId}")
    List<OutfitResponse> getOutfitsByUser(@PathVariable("userId") UUID userId);

    /**
     * Detalle completo del outfit: prendas por slot, colores, todos los scores
     * del OutfitAiAudit y metadata de la generación.
     */
    @GetMapping("/internal/outfits/{outfitId}")
    OutfitDetailResponse getOutfitById(@PathVariable("outfitId") UUID outfitId);

    /**
     * Actualiza el rating del usuario sobre un outfit [1–5].
     * Requiere userId para validar ownership antes de persistir.
     */
    @PatchMapping("/internal/outfits/{outfitId}/rating")
    OutfitResponse rateOutfit(
            @PathVariable("outfitId") UUID outfitId,
            @RequestParam("userId") UUID userId,
            @RequestBody OutfitRatingRequest request
    );
}