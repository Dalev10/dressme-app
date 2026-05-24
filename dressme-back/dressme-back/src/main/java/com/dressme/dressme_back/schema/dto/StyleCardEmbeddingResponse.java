package com.dressme.dressme_back.schema.dto;
 
import java.util.UUID;
 
public record StyleCardEmbeddingResponse(
    UUID id,
    String name,
    float[] embeddingVector
) {}