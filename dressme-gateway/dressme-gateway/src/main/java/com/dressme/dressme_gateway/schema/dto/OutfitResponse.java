package com.dressme.dressme_gateway.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vista resumida de un outfit para el grid del frontend.
 * Espejo del OutfitResponse de dressme-back — el Gateway lo proxea sin transformación.
 */
public record OutfitResponse(

        @JsonProperty("id")
        UUID id,

        @JsonProperty("userId")
        UUID userId,

        @JsonProperty("name")
        String name,

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

        @JsonProperty("matchScore")
        BigDecimal matchScore,

        @JsonProperty("affinityScore")
        BigDecimal affinityScore,

        @JsonProperty("totalScore")
        Double totalScore,

        @JsonProperty("rating")
        Integer rating,

        @JsonProperty("isAiProcessed")
        boolean isAiProcessed,

        @JsonProperty("createdAt")
        LocalDateTime createdAt

) {}
