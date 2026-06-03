package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detalle completo de un outfit para la vista individual.
 * Actúa como espejo de recepción del microservicio dressme-database.
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
        /**
         * Información esencial de una prenda dentro del outfit mapeada desde la DB.
         */
        public record ClothingSlot(
                @JsonProperty("id") UUID id,
                @JsonProperty("imageUrl") String imageUrl,
                @JsonProperty("categoryName") String categoryName,
                @JsonProperty("colorName") String colorName,
                @JsonProperty("colorHex") String colorHex
        ) {}
}