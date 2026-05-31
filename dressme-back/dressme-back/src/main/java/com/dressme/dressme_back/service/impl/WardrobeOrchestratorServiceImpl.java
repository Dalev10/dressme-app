package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.*;
import com.dressme.dressme_back.service.StorageService;
import com.dressme.dressme_back.service.WardrobeOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class WardrobeOrchestratorServiceImpl implements WardrobeOrchestratorService {

    private final StorageService storageService;
    private final RestClient     databaseClient;
    private final RestClient     aiClient;

    public WardrobeOrchestratorServiceImpl(
            StorageService storageService,
            RestClient.Builder restClientBuilder,
            @Value("${app.services.database-url}") String databaseUrl,
            @Value("${app.services.ai-url}")       String aiUrl
    ) {
        this.storageService = storageService;
        this.databaseClient = restClientBuilder.baseUrl(databaseUrl).build();
        this.aiClient       = restClientBuilder.baseUrl(aiUrl).build();
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    @Override
    public ClothingItemResponse uploadClothing(ClothingUploadRequest request, MultipartFile file) {
        log.info("Wardrobe: Upload para usuario {}", request.userId());

        String imageUrl = storageService.store(request.userId(), file);

        ClothingItemResponse created = databaseClient.post()
                .uri("/internal/wardrobe")
                .body(new ClothingCreateRequest(request.userId(), imageUrl))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException("dressme-database rechazó el registro");
                })
                .body(ClothingItemResponse.class);

        log.info("Wardrobe: Prenda {} registrada — disparando análisis IA", created.id());
        triggerVisionAnalysis(created.id(), imageUrl);
        return created;
    }

    @Async
    protected void triggerVisionAnalysis(UUID clothingId, String imageUrl) {
        log.info("Back-Wardrobe[async]: Enviando {} a dressme-ai", clothingId);
        try {
            VisionAnalysisResponse aiResponse = aiClient.post()
                    .uri("/ai/wardrobe/analyze")
                    .body(new VisionAnalysisRequest(clothingId, imageUrl))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException(
                                "dressme-ai falló para prenda " + clothingId);
                    })
                    .body(VisionAnalysisResponse.class);

            log.info("Back-Wardrobe[async]: Respuesta recibida de dressme-ai: clothingId={}, predictedCategoryId={}, predictedStyleId={}, predictedColorId={}, detectedHue={}, detectedSaturation={}, detectedLightness={}, predictedWeatherId={}, predictedOccasionId={}, confidence={}, aiProvider={}",
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

            log.info("Back-Wardrobe[async]: Enviando audit a dressme-database: clothingId={}, predictedColorId={}, detectedHue={}, detectedSaturation={}, detectedLightness={}",
                    auditRequest.clothingId(),
                    auditRequest.predictedColorId(),
                    auditRequest.detectedHue(),
                    auditRequest.detectedSaturation(),
                    auditRequest.detectedLightness());

            databaseClient.patch()
                    .uri("/internal/wardrobe/audit")
                    .body(auditRequest)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Back-Wardrobe[async]: Audit aplicado exitosamente para prenda {}", clothingId);

        } catch (Exception e) {
            log.error("Back-Wardrobe[async]: Error analizando prenda {}: {}", clothingId,
                    e.getMessage());
            // La prenda queda isProcessed=false; re-análisis futuro la puede recuperar.
        }
    }

    // ── Lista resumida ────────────────────────────────────────────────────────

    @Override
    public List<ClothingItemResponse> getWardrobe(UUID userId) {
        return databaseClient.get()
                .uri("/internal/wardrobe/user/{userId}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ClothingItemResponse>>() {});
    }

    // ── Detalle compuesto ─────────────────────────────────────────────────────

    @Override
    public ClothingDetailResponse getClothingDetail(UUID clothingId) {
        return databaseClient.get()
                .uri("/internal/wardrobe/{clothingId}", clothingId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException("Prenda no encontrada: " + clothingId);
                })
                .body(ClothingDetailResponse.class);
    }

    // ── Corrección manual ─────────────────────────────────────────────────────

    @Override
    public ClothingDetailResponse updateClothing(UUID clothingId, UUID userId,
                                                  ClothingEditRequest request) {
        log.info("Wardrobe: Corrección manual — prenda {} usuario {}", clothingId, userId);

        // Transformar ClothingEditRequest → ClothingUpdateRequest (contrato de dressme-database)
        ClothingUpdateRequest dbRequest = new ClothingUpdateRequest(
                request.typeId(),
                request.categoryId(),
                request.styleId()
        );

        return databaseClient.patch()
                .uri("/internal/wardrobe/{clothingId}?userId={userId}", clothingId, userId)
                .body(dbRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException(
                            "No se pudo actualizar la prenda: " + clothingId);
                })
                .body(ClothingDetailResponse.class);
    }

    
    @Override
    public WardrobeEditCatalogDTO getEditCatalog() {

        log.info("Wardrobe: Solicitando catálogo de edición");

        return databaseClient.get()
                .uri("/internal/wardrobe/catalog/edit")
                .retrieve()
                .body(WardrobeEditCatalogDTO.class);
   }




    // ── Eliminación ───────────────────────────────────────────────────────────

    @Override
    public void deleteClothing(UUID clothingId, UUID userId) {
        // 1. Obtener imageUrl antes de borrar (para limpiar el volumen)
        ClothingDetailResponse detail = getClothingDetail(clothingId);

        // 2. Eliminar en dressme-database (verifica ownership)
        databaseClient.delete()
                .uri("/internal/wardrobe/{clothingId}?userId={userId}", clothingId, userId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException(
                            "No se pudo eliminar la prenda: " + clothingId);
                })
                .toBodilessEntity();

        // 3. Eliminar imagen del volumen Docker
        if (detail != null && detail.imageUrl() != null) {
            storageService.delete(detail.imageUrl());
        }

        log.info("Wardrobe: Prenda {} eliminada", clothingId);
    }

    // ── Catálogo ──────────────────────────────────────────────────────────────

    @Override
    public CatalogDTO getCatalog() {
        return databaseClient.get()
                .uri("/internal/wardrobe/catalog")
                .retrieve()
                .body(CatalogDTO.class);
    }

    // ── Inner record de request a dressme-ai ─────────────────────────────────

    private record VisionAnalysisRequest(UUID clothingId, String imageUrl) {}
}