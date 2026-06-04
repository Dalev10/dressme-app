package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.*;
import com.dressme.dressme_back.service.StorageService;
import com.dressme.dressme_back.service.WardrobeOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class WardrobeOrchestratorServiceImpl implements WardrobeOrchestratorService {

    private final StorageService    storageService;
    private final RestClient        databaseClient;
    private final AsyncVisionService asyncVisionService;

    public WardrobeOrchestratorServiceImpl(
            StorageService storageService,
            RestClient.Builder restClientBuilder,
            AsyncVisionService asyncVisionService,
            @Value("${app.services.database-url}") String databaseUrl
    ) {
        this.storageService      = storageService;
        this.databaseClient      = restClientBuilder.baseUrl(databaseUrl).build();
        this.asyncVisionService  = asyncVisionService;
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
        asyncVisionService.triggerVisionAnalysis(created.id(), imageUrl);
        return created;
    }

    // ── Lista resumida ────────────────────────────────────────────────────────

    @Override
    public List<ClothingItemResponse> getWardrobe(UUID userId) {
        return databaseClient.get()
                .uri("/internal/wardrobe/user/{userId}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ClothingItemResponse>>() {});
    }

    // ── Guardarropa para outfit generation (con embeddings) ───────────────────

    @Override
    public List<ClothingEmbeddingInfo> getWardrobeForOutfit(UUID userId, UUID occasionId, UUID weatherId) {
        log.info("Wardrobe: getWardrobeForOutfit — usuario {}, ocasión {}, clima {}", userId, occasionId, weatherId);
        return databaseClient.get()
                .uri("/internal/wardrobe/user/{userId}/for-outfit?occasionId={occasionId}&weatherId={weatherId}",
                        userId, occasionId, weatherId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ClothingEmbeddingInfo>>() {});
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
                    throw new RuntimeException("No se pudo actualizar la prenda: " + clothingId);
                })
                .body(ClothingDetailResponse.class);
    }

    // ── Eliminación ───────────────────────────────────────────────────────────

    @Override
    public void deleteClothing(UUID clothingId, UUID userId) {
        ClothingDetailResponse detail = getClothingDetail(clothingId);

        databaseClient.delete()
                .uri("/internal/wardrobe/{clothingId}?userId={userId}", clothingId, userId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException("No se pudo eliminar la prenda: " + clothingId);
                })
                .toBodilessEntity();

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

    @Override
    public WardrobeEditCatalogDTO getEditCatalog() {
        log.info("Wardrobe: Solicitando catálogo de edición");
        return databaseClient.get()
                .uri("/internal/wardrobe/catalog/edit")
                .retrieve()
                .body(WardrobeEditCatalogDTO.class);
    }
}
