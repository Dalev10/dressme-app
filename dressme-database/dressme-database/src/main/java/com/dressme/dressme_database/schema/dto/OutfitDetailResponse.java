package com.dressme.dressme_database.schema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detalle completo de un outfit para la vista individual.
 * Combina tbl_outfits + tbl_outfit_ai_audit + prendas resueltas.
 *
 * ClothingSlot incluye colorName y colorHex resueltos desde
 * tbl_clothing_ai_audit.predicted_color → tbl_colors (HSL → hex).
 */
public record OutfitDetailResponse(

    // ── Outfit ────────────────────────────────────────────────────────────────
    UUID id,
    UUID userId,
    String name,
    Integer rating,
    boolean isAiProcessed,
    LocalDateTime createdAt,

    // ── Contexto ──────────────────────────────────────────────────────────────
    UUID dressCodeId,
    String dressCodeName,

    UUID occasionId,
    String occasionName,

    UUID weatherId,
    String weatherName,

    // ── Prendas ───────────────────────────────────────────────────────────────
    List<ClothingSlot> clothing,

    // ── Scores del audit IA ───────────────────────────────────────────────────
    BigDecimal matchScore,
    BigDecimal affinityScore

) {

    /**
     * Información esencial de una prenda dentro del outfit.
     * colorHex calculado desde HSL del audit de la prenda (hslToHex en OutfitServiceImpl).
     * null si la prenda aún no fue procesada por la IA.
     */
    public record ClothingSlot(
        UUID   id,
        String imageUrl,
        String categoryName,
        String colorName,
        String colorHex
    ) {}
}