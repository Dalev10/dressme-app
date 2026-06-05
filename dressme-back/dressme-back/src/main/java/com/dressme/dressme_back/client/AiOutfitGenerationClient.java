package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.AiOutfitGenerationRequest;
import com.dressme.dressme_back.schema.dto.AiOutfitGenerationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client para el endpoint de outfit generation en dressme-ai.
 *
 * dressme-ai recibe el guardarropa filtrado y el embedding del dress code,
 * genera combinaciones de outfits y calcula dresscode_score por candidato.
 *
 * dressme-back completa el scoring (color, taste, trend) y persiste.
 *
 * Timeout recomendado: 30s — la generación de candidatos con Gemini
 * puede tardar más que los endpoints de scoring puntuales.
 */
@FeignClient(name = "dressme-ai-outfit-client", url = "${app.services.ai-url}")
public interface AiOutfitGenerationClient {

    @PostMapping("/ai/outfit/generate/orchestrated")
    AiOutfitGenerationResponse generateOutfits(@RequestBody AiOutfitGenerationRequest request);
}