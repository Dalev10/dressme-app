package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record ComputeTasteVectorRequest(
    @JsonProperty("user_id") UUID userId,
    List<EmbeddingItem> selections
) {
    public record EmbeddingItem(
        @JsonProperty("style_card_id") UUID styleCardId,
        float[] embedding,
        String reaction   // "LIKE" | "DISLIKE" | "SKIP"
    ) {}
}