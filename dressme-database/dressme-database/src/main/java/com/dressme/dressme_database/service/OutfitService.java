package com.dressme.dressme_database.service;
import com.dressme.dressme_database.schema.dto.*;
import java.util.List;
import java.util.UUID;

public interface OutfitService {
    OutfitDressCodeResponse getDressCode(UUID outfitId);
    /**
     * Persiste un outfit generado por el motor de recomendación.
     * @Transactional: crea Outfit + ClothingOutfits + OutfitAiAudit en una sola unidad.
     */
    OutfitResponse createOutfit(OutfitCreateRequest request);

    /**
     * Actualiza affinityScore y totalScore en el audit tras el ScoreEngine.
     * Llamado por dressme-back en el paso de scoring post-persistencia.
     */
    void updateScores(UUID outfitId, OutfitScoreUpdateRequest request);

    /**
     * Lista de outfits del usuario ordenados por totalScore descendente.
     */
    List<OutfitResponse> getOutfitsByUser(UUID userId);

    /**
     * Detalle completo: metadatos + prendas resueltas + scores del audit.
     * Lanza RuntimeException (→ 404) si no existe.
     */
    OutfitDetailResponse getOutfitDetail(UUID outfitId);

    /**
     * Registra la calificación del usuario [1–5].
     * Lanza RuntimeException (→ 404) si no existe o no pertenece al usuario.
     */
    OutfitResponse rateOutfit(UUID outfitId, UUID userId, OutfitRatingRequest request);

    /**
     * Elimina outfit + ClothingOutfits + OutfitAiAudit respetando FK constraints.
     * Lanza RuntimeException (→ 404) si no existe o no pertenece al usuario.
     */
    void deleteOutfit(UUID outfitId, UUID userId);
}