package com.dressme.dressme_database.repository;

import com.dressme.dressme_database.model.Clothing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClothingRepository extends JpaRepository<Clothing, UUID> {

    /**
     * Todas las prendas de un usuario, más recientes primero.
     * Usado para listar el guardarropa completo.
     */
    List<Clothing> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Prendas pendientes de análisis IA.
     * El scheduler / worker de Vision las consume en lotes.
     */
    List<Clothing> findByIsProcessedFalse();

    /**
     * Prendas de un usuario filtradas por categoría.
     * Útil cuando el frontend quiere mostrar solo "Tops", "Bottoms", etc.
     */
    List<Clothing> findByUserIdAndCategoryIdOrderByCreatedAtDesc(UUID userId, UUID categoryId);

    /**
     * Conteo rápido de prendas de un usuario (para stats del perfil).
     */
    long countByUserId(UUID userId);

    /**
     * Verifica si una prenda pertenece a un usuario concreto.
     * Evita cargar la entidad completa solo para una verificación de ownership.
     */
    @Query("SELECT COUNT(c) > 0 FROM Clothing c WHERE c.id = :clothingId AND c.user.id = :userId")
    boolean existsByIdAndUserId(@Param("clothingId") UUID clothingId, @Param("userId") UUID userId);

    // ── Outfit Generation ─────────────────────────────────────────────────────

    /**
     * Prendas de un usuario aptas para generación de outfits, filtradas por
     * ocasión y clima.
     *
     * Criterios de inclusión:
     *   - embedding_vector IS NOT NULL  → la prenda ya fue vectorizada
     *   - is_embedding_stale = false    → el vector está vigente (no hay corrección pendiente)
     *   - is_processed = true           → la IA completó el análisis Vision
     *   - join con tbl_clothes_occasions → filtra por occasionId
     *   - join con tbl_clothes_weather  → filtra por weatherId
     *
     * El query devuelve la categoría hoja (cat.name) y la padre (cat.parent.name).
     * El slot canónico (TOP/BOTTOM/OUTERWEAR/FOOTWEAR/ONEPIECE) lo resuelve
     * ClothingServiceImpl con CategorySlotMapper, que maneja el caso "Activewear"
     * (cuyos hijos caen en slots distintos) y excluye accesorios.
     *
     * Solo se retornan los campos necesarios para ClothingEmbeddingDTO via proyección.
     * El embedding_vector se hidrata en ClothingServiceImpl en un segundo paso
     * (findById por los IDs obtenidos aquí) para evitar problemas de serialización
     * del tipo vector(1536) en proyecciones planas.
     *
     * @param userId     propietario del guardarropa
     * @param occasionId ocasión para la que se genera el outfit
     * @param weatherId  clima para el que se genera el outfit
     */
    @Query("""
        SELECT
            c.id                                            AS clothingId,
            cat.name                                        AS categoryName,
            cat.parent.name                                 AS parentName,
            audit.detectedHue                               AS detectedHue,
            audit.detectedSaturation                        AS detectedSaturation,
            audit.detectedLightness                         AS detectedLightness,
            occ.name                                        AS occasionName,
            w.name                                          AS weatherName
        FROM Clothing c
        JOIN ClothingAiAudit audit ON audit.clothing.id = c.id
        JOIN c.category cat
        JOIN ClothingOccasion co  ON co.clothing.id = c.id
        JOIN co.occasion occ
        JOIN ClothingWeather cw   ON cw.clothing.id = c.id
        JOIN cw.weather w
        WHERE c.user.id          = :userId
          AND c.isProcessed       = true
          AND c.isEmbeddingStale  = false
          AND c.embeddingVector  IS NOT NULL
          AND occ.id             = :occasionId
          AND w.id               = :weatherId
        """)
    List<ClothingWithEmbeddingProjection> findForOutfitGeneration(
            @Param("userId")     UUID userId,
            @Param("occasionId") UUID occasionId,
            @Param("weatherId")  UUID weatherId
    );

    /**
     * Carga solo los IDs y embeddingVector de las prendas indicadas.
     *
     * Usado por ClothingServiceImpl como segundo paso de la hidratación:
     * findForOutfitGeneration devuelve los metadatos escalares via proyección,
     * y este query trae los embeddings de las mismas prendas por ID para
     * completar el ClothingEmbeddingDTO.
     *
     * Se carga la entidad Clothing completa (solo embedding_vector y id se usan),
     * lo que garantiza que el tipo vector(1536) se deserializa correctamente
     * por el @JdbcTypeCode(SqlTypes.VECTOR) de Hibernate.
     *
     * @param ids lista de clothingId retornados por findForOutfitGeneration
     */
    @Query("SELECT c FROM Clothing c WHERE c.id IN :ids")
    List<Clothing> findEmbeddingsByIds(@Param("ids") List<UUID> ids);

    /**
     * Candidatos para Step 0 del orquestador: todas las prendas procesadas que
     * coincidan con ocasión y clima, SIN filtrar por embedding.
     *
     * Incluye prendas con embeddingVector = null o isEmbeddingStale = true.
     * El orquestador detecta cuáles necesitan reparación y llama a dressme-ai
     * para vectorizarlas antes de la generación real.
     *
     * @param userId     propietario del guardarropa
     * @param occasionId ocasión para la que se genera el outfit
     * @param weatherId  clima para el que se genera el outfit
     */
    @Query("""
        SELECT
            c.id                                            AS clothingId,
            cat.name                                        AS categoryName,
            cat.parent.name                                 AS parentName,
            audit.detectedHue                               AS detectedHue,
            audit.detectedSaturation                        AS detectedSaturation,
            audit.detectedLightness                         AS detectedLightness,
            occ.name                                        AS occasionName,
            w.name                                          AS weatherName
        FROM Clothing c
        JOIN ClothingAiAudit audit ON audit.clothing.id = c.id
        JOIN c.category cat
        JOIN ClothingOccasion co  ON co.clothing.id = c.id
        JOIN co.occasion occ
        JOIN ClothingWeather cw   ON cw.clothing.id = c.id
        JOIN cw.weather w
        WHERE c.user.id          = :userId
          AND c.isProcessed       = true
          AND occ.id             = :occasionId
          AND w.id               = :weatherId
        """)
    List<ClothingWithEmbeddingProjection> findCandidatesForOutfitGeneration(
            @Param("userId")     UUID userId,
            @Param("occasionId") UUID occasionId,
            @Param("weatherId")  UUID weatherId
    );
}