package com.dressme.dressme_back.schema.dto;
 
import java.util.List;
import java.util.UUID;
 
public record ComputeTasteVectorRequest(
    UUID userId,
    List<EmbeddingItem> selections
) {
    public record EmbeddingItem(
        UUID styleCardId,
        float[] embedding,
        String reaction   // "LIKE" | "DISLIKE" | "SKIP"
    ) {}
}