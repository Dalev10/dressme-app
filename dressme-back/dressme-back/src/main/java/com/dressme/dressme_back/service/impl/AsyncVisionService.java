package com.dressme.dressme_back.service.impl;

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
 * Análisis de visión ejecutado de forma asíncrona.
 *
 * Flujo:
 *   1. POST /ai/wardrobe/analyze       → categoría, estilo, color, clima, ocasión (UUIDs)
 *   2. PATCH /internal/wardrobe/audit  → persiste el audit en dressme-database
 *
 * Nota:
 *   El embedding vectorial ya no se genera aquí.
 *   Ahora lo resuelve el orquestador en Step 0, antes de generar outfits.
 *
 * Existe como bean separado para que @Async sea proxeado correctamente por
 * Spring AOP (self-invocation no es interceptada por el proxy).
 */
@Service
@Slf4j
public class AsyncVisionService {

    private final RestClient databaseClient;
    private final RestClient aiVisionClient;

    public AsyncVisionService(
            RestClient.Builder restClientBuilder,
            @org.springframework.beans.factory.annotation.Value("${app.services.database-url}") String databaseUrl,
            @org.springframework.beans.factory.annotation.Value("${app.services.ai-url}")       String aiUrl
    ) {
        this.databaseClient  = restClientBuilder.clone().baseUrl(databaseUrl).build();


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

                if (aiResponse == null){
                        log.warn("Back-Wardrobe[async]: Respuesta nula de visión para prenda {}", clothingId);
                        return;
                }

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
                            aiResponse.predictedWeatherIds(),
                            aiResponse.predictedOccasionIds(),
                            aiResponse.confidenceScore(),
                            aiResponse.aiProvider()))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Back-Wardrobe[async]: Audit guardado para prenda {}", clothingId);
            log.info("Back-Wardrobe[async]: Pipeline de visión finalizado para prenda {}", clothingId);            
            
        } catch (Exception e) {
            log.error("Back-Wardrobe[async]: Error en pipeline de visión para prenda {}: {}", clothingId, e.getMessage());
        }
    }

    private record VisionAnalysisRequest(UUID clothingId, String imageUrl) {}
}
