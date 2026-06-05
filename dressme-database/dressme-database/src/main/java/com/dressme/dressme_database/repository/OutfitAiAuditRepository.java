package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.OutfitAiAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutfitAiAuditRepository extends JpaRepository<OutfitAiAudit, UUID> {

    // ── Gestión CRUD ──────────────────────────────────────────────────────────

    /**
     * Audit IA de un outfit específico.
     * Relación 1:1 con Outfit garantizada por unique=true en la FK.
     * Usado por OutfitServiceImpl para leer scores y vector del outfit.
     */
    Optional<OutfitAiAudit> findByOutfitId(UUID outfitId);

    /**
     * Verifica si ya existe un audit para un outfit.
     * Permite decidir entre INSERT y UPDATE en el flujo de scoring.
     */
    boolean existsByOutfitId(UUID outfitId);

    // ── Motor de recomendación ────────────────────────────────────────────────

    /**
     * Projection para el resultado de similitud coseno.
     * outfitId   → UUID del outfit analizado
     * similarity → valor [0.0–1.0] donde 1.0 = idéntico al gusto del usuario
     */
    interface TasteSimilarityProjection {
        UUID   getOutfitId();
        Double getSimilarity();
    }

    /**
     * Calcula la similitud coseno entre el outfit_vector de cada outfit
     * y el taste_vector del usuario usando pgvector (<=>).
     *
     * El operador <=> devuelve distancia coseno [0–2], donde 0 = idénticos.
     * La fórmula 1 - (<=> / 2) no es correcta; la correcta es:
     *   GREATEST(0, LEAST(1, 1 - (u.taste_vector <=> a.outfit_vector)))
     * que normaliza la distancia coseno a similitud [0–1] y la clampea
     * para evitar valores negativos por imprecisión de punto flotante.
     *
     * Devuelve NULL para outfits sin outfit_vector (aún no vectorizados).
     * El OrquestadorService los filtra antes de rankear.
     *
     * @param userId    UUID del usuario — para obtener su taste_vector
     * @param outfitIds Lista de outfit IDs a comparar (lote del generador)
     */
    @Query(value = """
        SELECT a.outfit_id AS outfitId,
               CASE
                   WHEN a.outfit_vector IS NULL THEN NULL
                   ELSE GREATEST(0, LEAST(1, 1 - (u.taste_vector <=> a.outfit_vector)))
               END AS similarity
          FROM tbl_outfit_ai_audit a
          JOIN tbl_user_taste_profile u ON u.user_id = :userId
         WHERE a.outfit_id IN (:outfitIds)
        """, nativeQuery = true)
    List<TasteSimilarityProjection> findTasteSimilarities(
            @Param("userId")    UUID userId,
            @Param("outfitIds") List<UUID> outfitIds
    );
}