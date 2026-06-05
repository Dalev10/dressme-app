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
 *   cat.name          → getCategoryName()   (categoría hoja, ej. "T-Shirt")
 *   cat.parent.name   → getParentName()     (categoría padre, null si la hoja es raíz)
 *   audit.detectedHue → getDetectedHue()
 *   audit.detectedSaturation → getDetectedSaturation()
 *   audit.detectedLightness  → getDetectedLightness()
 *   o.name            → getOccasionName()
 *   w.name            → getWeatherName()
 *
 * El slot canónico (TOP/BOTTOM/OUTERWEAR/FOOTWEAR/ONEPIECE) se resuelve en
 * ClothingServiceImpl con CategorySlotMapper.toSlot(categoryName, parentName),
 * que maneja el caso de "Activewear" (hijos en slots distintos).
 */
public interface ClothingWithEmbeddingProjection {

    /** UUID de la prenda (tbl_clothes.id) */
    UUID getClothingId();

    /** Nombre de la categoría hoja de la prenda (ej. "T-Shirt", "Sports Bra"). */
    String getCategoryName();

    /** Nombre de la categoría padre, o null si la categoría hoja es raíz. */
    String getParentName();

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