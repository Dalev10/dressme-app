package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.AiEmbeddingClothingClient;
import com.dressme.dressme_back.schema.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Análisis de visión + embedding vectorial, ejecutados de forma asíncrona.
 *
 * Flujo completo por prenda (se ejecuta en background tras el upload):
 *   1. POST /ai/wardrobe/analyze       → categoría, estilo, color, clima, ocasión (UUIDs)
 *   2. PATCH /internal/wardrobe/audit  → persiste el audit en dressme-database
 *   3. GET /internal/wardrobe/{id}     → resuelve nombres para el embedding
 *   4. POST /ai/outfit/embed-clothing  → genera vector semántico 1536 dims
 *   5. PATCH /internal/wardrobe/{id}/embedding → persiste el vector en tbl_clothes
 *
 * Existe como bean separado para que @Async sea proxeado correctamente por
 * Spring AOP (self-invocation no es interceptada por el proxy).
 */
@Service
@Slf4j
public class AsyncVisionService {

    private final RestClient                databaseClient;
    private final RestClient                aiVisionClient;
    private final AiEmbeddingClothingClient embeddingClient;

    public AsyncVisionService(
            RestClient.Builder restClientBuilder,
            AiEmbeddingClothingClient embeddingClient,
            @org.springframework.beans.factory.annotation.Value("${app.services.database-url}") String databaseUrl,
            @org.springframework.beans.factory.annotation.Value("${app.services.ai-url}")       String aiUrl
    ) {
        this.databaseClient  = restClientBuilder.clone().baseUrl(databaseUrl).build();
        this.embeddingClient = embeddingClient;

        // Gemini Vision puede tardar hasta 30s. Timeout explícito de 35s
        // para evitar el límite de 10s heredado de OkHttp vía Feign en classpath.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(35));
        this.aiVisionClient = RestClient.builder().requestFactory(factory).baseUrl(aiUrl).build();
    }

    @Async
    public void triggerVisionAnalysis(UUID clothingId, String imageUrl) {
        log.info("Back-Wardrobe[async]: Iniciando análisis completo para prenda {}", clothingId);
        try {
            // ── Paso 1: análisis de visión ────────────────────────────────────
            VisionAnalysisResponse aiResponse = aiVisionClient.post()
                    .uri("/ai/wardrobe/analyze")
                    .body(new VisionAnalysisRequest(clothingId, imageUrl))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("dressme-ai (vision) falló para prenda " + clothingId);
                    })
                    .body(VisionAnalysisResponse.class);

            // ── Paso 2: guardar audit ─────────────────────────────────────────
            databaseClient.patch()
                    .uri("/internal/wardrobe/audit")
                    .body(new AuditUpdateRequest(
                            aiResponse.clothingId(),
                            aiResponse.predictedCategoryId(),
                            aiResponse.predictedStyleId(),
                            aiResponse.predictedColorId(),
                            aiResponse.detectedHue(),
                            aiResponse.detectedSaturation(),
                            aiResponse.detectedLightness(),
                            aiResponse.predictedWeatherId(),
                            aiResponse.predictedOccasionId(),
                            aiResponse.confidenceScore(),
                            aiResponse.aiProvider()))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Back-Wardrobe[async]: Audit guardado para prenda {}", clothingId);

            // ── Paso 3: obtener nombres resueltos para el embedding ───────────
            ClothingDetailResponse detail = databaseClient.get()
                    .uri("/internal/wardrobe/{clothingId}", clothingId)
                    .retrieve()
                    .body(ClothingDetailResponse.class);

            if (detail == null || detail.categoryName() == null) {
                log.warn("Back-Wardrobe[async]: No se pudo obtener detalle para embedding de prenda {}", clothingId);
                return;
            }

            // ── Paso 4: generar embedding vectorial ───────────────────────────
            ClothingEmbeddingResponse embResponse = embeddingClient.embedClothing(
                    new ClothingEmbeddingRequest(
                            clothingId,
                            detail.categoryName(),
                            detail.styleName()   != null ? detail.styleName()   : "Casual",
                            detail.colorName()   != null ? detail.colorName()   : "Neutral",
                            detail.weatherName() != null ? detail.weatherName() : "Mild",
                            detail.occasionName() != null ? detail.occasionName() : "Everyday"
                    ));

            log.info("Back-Wardrobe[async]: Embedding generado para prenda {} ({} dims)",
                    clothingId, embResponse.embeddingVector().size());

            // ── Paso 5: persistir embedding en dressme-database ───────────────
            databaseClient.patch()
                    .uri("/internal/wardrobe/{clothingId}/embedding", clothingId)
                    .body(new ClothingEmbeddingUpdateRequest(clothingId, embResponse.embeddingVector()))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Back-Wardrobe[async]: Pipeline completo para prenda {}", clothingId);

        } catch (Exception e) {
            log.error("Back-Wardrobe[async]: Error en pipeline de prenda {}: {}", clothingId, e.getMessage());
        }
    }

    private record VisionAnalysisRequest(UUID clothingId, String imageUrl) {}
}
