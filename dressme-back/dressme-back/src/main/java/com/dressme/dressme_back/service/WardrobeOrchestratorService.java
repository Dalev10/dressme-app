package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Orquestador completo del flujo Wardrobe en dressme-back.
 */
public interface WardrobeOrchestratorService {

    /** Upload: guarda imagen + registra prenda + dispara análisis IA en @Async. */
    ClothingItemResponse uploadClothing(ClothingUploadRequest request, MultipartFile file);

    /** Lista resumida del guardarropa (ClothingItemResponse — para el grid). */
    List<ClothingItemResponse> getWardrobe(UUID userId);

    /**
     * Detalle compuesto de una prenda (ClothingDetailResponse — para view/edit).
     * Proxy a GET /internal/wardrobe/{clothingId} en dressme-database.
     */
    ClothingDetailResponse getClothingDetail(UUID clothingId);

    /**
     * Corrección manual de los campos predichos por la IA.
     * Proxy a PATCH /internal/wardrobe/{clothingId} en dressme-database.
     */
    ClothingDetailResponse updateClothing(UUID clothingId, UUID userId,
                                          ClothingEditRequest request);

    /** Elimina prenda en dressme-database + imagen del volumen Docker. */
    void deleteClothing(UUID clothingId, UUID userId);

    /** Catálogo de referencia: categorías + estilos + colores + ocasiones + climas. */
    CatalogDTO getCatalog();

    WardrobeEditCatalogDTO getEditCatalog();

    /** Guardarropa filtrado por ocasión y clima, con embeddings, para outfit generation. */
    List<ClothingEmbeddingInfo> getWardrobeForOutfit(UUID userId, UUID occasionId, UUID weatherId);

    /** Candidatos para Step 0: incluye prendas con embedding null o stale. */
    List<ClothingEmbeddingInfo> getCandidatesForOutfit(UUID userId, UUID occasionId, UUID weatherId);
}