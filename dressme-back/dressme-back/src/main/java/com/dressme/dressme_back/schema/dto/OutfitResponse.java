package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vista resumida de un outfit para el grid del frontend.
 * Replicado localmente en dressme-back para actuar como contrato de recepción 
 * desde dressme-database y de salida hacia el dressme-gateway.
 */
public record OutfitResponse(

        @JsonProperty("id")
        UUID id,

        @JsonProperty("userId")
        UUID userId,

        @JsonProperty("name")
        String name,

        /** * URLs de imagen de las prendas. 
         * Se usa "clothingImageUrls" para mapear de forma idéntica el JSON de dressme-database.
         */
        @JsonProperty("clothingImageUrls")
        List<String> clothingImageUrls,

        @JsonProperty("clothingIds")
        List<UUID> clothingIds,

        @JsonProperty("occasionId")
        UUID occasionId,

        @JsonProperty("occasionName")
        String occasionName,

        @JsonProperty("weatherId")
        UUID weatherId,

        @JsonProperty("weatherName")
        String weatherName,

        /** Score compuesto [0.00–1.00] */
        @JsonProperty("matchScore")
        BigDecimal matchScore,

        /** * Afinidad con el taste_vector [0.00–1.00].
         */
        @JsonProperty("affinityScore")
        BigDecimal affinityScore,

        /**
         * Score total compuesto calculado por el ScoreEngine [0.0–1.0].
         * Es el valor de ranking definitivo del outfit.
         * Nullable: outfits legacy anteriores al ScoreEngine no lo tienen.
         */
        @JsonProperty("totalScore")
        Double totalScore,

        @JsonProperty("rating")
        Integer rating,

        @JsonProperty("isAiProcessed")
        boolean isAiProcessed,

        @JsonProperty("createdAt")
        LocalDateTime createdAt

) {}