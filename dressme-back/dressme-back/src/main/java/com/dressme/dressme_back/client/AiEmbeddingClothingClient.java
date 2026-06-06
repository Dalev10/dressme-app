package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.ClothingEmbeddingRequest;
import com.dressme.dressme_back.schema.dto.ClothingEmbeddingResponse;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client para vectorizar una prenda individual en dressme-ai.
 *
 * Llamado desde AsyncVisionService tras guardar el audit de visión.
 * El embedding resultante se persiste en tbl_clothes.embedding_vector
 * via PATCH /internal/wardrobe/{clothingId}/embedding en dressme-database.
 *
 * Timeout: gemini-embedding-001 responde en ~2s, el timeout por defecto
 * de Feign/OkHttp (10s) es suficiente para este endpoint.
 */
@FeignClient(name = "dressme-ai-embedding-client", url = "${app.services.ai-url}")
public interface AiEmbeddingClothingClient {

    @PostMapping("/ai/outfit/embed-clothing")
    ClothingEmbeddingResponse embedClothing(@RequestBody ClothingEmbeddingRequest request);

    @PostMapping("/ai/outfit/embed-clothing/batch")
    List<ClothingEmbeddingResponse> embedClothingBatch(@RequestBody List<ClothingEmbeddingRequest> requests);
}
