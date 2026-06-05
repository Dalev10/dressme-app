package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.ClothingEmbeddingInfo;
import com.dressme.dressme_back.schema.dto.OutfitDressCodeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "dressme-database-wardrobe-client", url = "${app.services.database-url}")
public interface DatabaseWardrobeClient {

    @GetMapping("/internal/wardrobe/{outfitId}/dress-code")
    OutfitDressCodeResponse getDressCode(@PathVariable("outfitId") UUID outfitId);

    /**
     * Prendas del guardarropa del usuario listas para outfit generation.
     *
     * Solo retorna prendas con:
     *   - is_processed = true
     *   - embedding_vector IS NOT NULL
     *   - is_embedding_stale = false
     *   - join con ocasión y clima del request
     *
     * Implementado en dressme-database como GET /internal/wardrobe/user/{userId}/for-outfit.
     */
    @GetMapping("/internal/wardrobe/user/{userId}/for-outfit")
    List<ClothingEmbeddingInfo> getWardrobeForOutfit(
            @PathVariable("userId") UUID userId,
            @RequestParam("occasionId") UUID occasionId,
            @RequestParam("weatherId") UUID weatherId
    );
}