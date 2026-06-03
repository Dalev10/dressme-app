package com.dressme.dressme_database.repository;

import java.util.UUID;

/**
 * Proyección JPA para el query de prendas aptas para generación de outfits.
 *
 * Evita cargar la entidad Clothing completa (con sus lazy relations) cuando
 * solo se necesitan los campos escalares para construir ClothingEmbeddingDTO.
 *
 * El embedding_vector NO se incluye aquí intencionalmente:
 * el tipo vector(1536) de pgvector no se mapea bien como proyección plana en
 * algunos drivers. ClothingServiceImpl lo hidrata en un segundo paso con
 * ClothingRepository.findEmbeddingsByIds() sobre las entidades Clothing cargadas
 * por ID. Esto mantiene la proyección ligera y evita problemas de serialización.
 *
 * Campos del query JPQL correspondiente (findForOutfitGeneration):
 *   c.id              → getClothingId()
 *   cat.parent.name   → getSlot()          (nombre de categoría padre)
 *   audit.detectedHue → getDetectedHue()
 *   audit.detectedSaturation → getDetectedSaturation()
 *   audit.detectedLightness  → getDetectedLightness()
 *   o.name            → getOccasionName()
 *   w.name            → getWeatherName()
 */
public interface ClothingWithEmbeddingProjection {

    /** UUID de la prenda (tbl_clothes.id) */
    UUID getClothingId();

    /**
     * Nombre del slot derivado de la categoría padre.
     * Si la categoría es raíz (parent = null), el query usa cat.name directamente.
     * Ver COALESCE en findForOutfitGeneration.
     */
    String getSlot();

    /** Hue HSL detectado por Vision [0–360]. Puede ser null si el audit no lo registró. */
    Integer getDetectedHue();

    /** Saturación HSL detectada por Vision [0–100]. */
    Integer getDetectedSaturation();

    /** Luminosidad HSL detectada por Vision [0–100]. */
    Integer getDetectedLightness();

    /** Nombre de la ocasión de la prenda (join con tbl_clothes_occasions → tbl_occasions). */
    String getOccasionName();

    /** Nombre del clima de la prenda (join con tbl_clothes_weather → tbl_weather). */
    String getWeatherName();
}