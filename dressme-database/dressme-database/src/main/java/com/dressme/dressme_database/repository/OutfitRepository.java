package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, UUID> {

    /**
     * Outfits de un usuario ordenados por fecha descendente.
     * Para el historial completo del guardarropa.
     */
    List<Outfit> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Outfits de un usuario ordenados por totalScore descendente.
     * NULLS LAST garantiza que outfits legacy (sin totalScore) queden al final.
     */
    @Query("""
        SELECT o FROM Outfit o
        JOIN OutfitAiAudit a ON a.outfit.id = o.id
        WHERE o.user.id = :userId
        ORDER BY a.totalScore DESC NULLS LAST
        """)
    List<Outfit> findTopByUserIdOrderByAffinityScore(@Param("userId") UUID userId);

    /**
     * Outfits favoritos del usuario (rating no nulo), ordenados por rating desc.
     * Para mostrar la sección "Mis favoritos".
     */
    List<Outfit> findByUserIdAndRatingIsNotNullOrderByRatingDesc(UUID userId);

    /**
     * Verifica ownership sin cargar la entidad completa.
     */
    @Query("SELECT COUNT(o) > 0 FROM Outfit o WHERE o.id = :outfitId AND o.user.id = :userId")
    boolean existsByIdAndUserId(
            @Param("outfitId") UUID outfitId,
            @Param("userId")   UUID userId
    );
}