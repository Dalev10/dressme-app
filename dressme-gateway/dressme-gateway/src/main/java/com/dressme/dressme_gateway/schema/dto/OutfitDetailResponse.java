package com.dressme.dressme_gateway.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detalle completo de un outfit para la vista individual.
 * Espejo del OutfitDetailResponse de dressme-back — el Gateway lo proxea sin transformación.
 */
public record OutfitDetailResponse(

        @JsonProperty("id") UUID id,
        @JsonProperty("userId") UUID userId,
        @JsonProperty("name") String name,
        @JsonProperty("rating") Integer rating,
        @JsonProperty("isAiProcessed") boolean isAiProcessed,
        @JsonProperty("createdAt") LocalDateTime createdAt,

        @JsonProperty("dressCodeId") UUID dressCodeId,
        @JsonProperty("dressCodeName") String dressCodeName,

        @JsonProperty("occasionId") UUID occasionId,
        @JsonProperty("occasionName") String occasionName,

        @JsonProperty("weatherId") UUID weatherId,
        @JsonProperty("weatherName") String weatherName,

        @JsonProperty("clothing") List<ClothingSlot> clothing,

        @JsonProperty("matchScore") BigDecimal matchScore,
        @JsonProperty("affinityScore") BigDecimal affinityScore

) {
        public record ClothingSlot(
                @JsonProperty("id") UUID id,
                @JsonProperty("imageUrl") String imageUrl,
                @JsonProperty("categoryName") String categoryName,
                @JsonProperty("colorName") String colorName,
                @JsonProperty("colorHex") String colorHex
        ) {}
}
