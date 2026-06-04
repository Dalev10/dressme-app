package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.AuditUpdateRequest;
import com.dressme.dressme_back.schema.dto.VisionAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

/**
 * Servicio dedicado para el análisis de visión asíncrono.
 *
 * Existe como bean separado para que @Async sea interceptado por el proxy
 * de Spring AOP. Si este método viviera en WardrobeOrchestratorServiceImpl
 * y fuera llamado con "this.", Spring no proxearía la llamada y la ejecución
 * sería síncrona, bloqueando el upload hasta el timeout de la IA (~10s).
 */
@Service
@Slf4j
public class AsyncVisionService {

    private final RestClient databaseClient;
    private final RestClient aiClient;

    public AsyncVisionService(
            RestClient.Builder restClientBuilder,
            @org.springframework.beans.factory.annotation.Value("${app.services.database-url}") String databaseUrl,
            @org.springframework.beans.factory.annotation.Value("${app.services.ai-url}") String aiUrl
    ) {
        this.databaseClient = restClientBuilder.clone().baseUrl(databaseUrl).build();
        this.aiClient       = restClientBuilder.clone().baseUrl(aiUrl).build();
    }

    @Async
    public void triggerVisionAnalysis(UUID clothingId, String imageUrl) {
        log.info("Back-Wardrobe[async]: Enviando {} a dressme-ai", clothingId);
        try {
            VisionAnalysisResponse aiResponse = aiClient.post()
                    .uri("/ai/wardrobe/analyze")
                    .body(new VisionAnalysisRequest(clothingId, imageUrl))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("dressme-ai falló para prenda " + clothingId);
                    })
                    .body(VisionAnalysisResponse.class);

            AuditUpdateRequest auditRequest = new AuditUpdateRequest(
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
                    aiResponse.aiProvider());

            databaseClient.patch()
                    .uri("/internal/wardrobe/audit")
                    .body(auditRequest)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Back-Wardrobe[async]: Audit aplicado exitosamente para prenda {}", clothingId);

        } catch (Exception e) {
            log.error("Back-Wardrobe[async]: Error analizando prenda {}: {}", clothingId, e.getMessage());
        }
    }

    private record VisionAnalysisRequest(UUID clothingId, String imageUrl) {}
}
