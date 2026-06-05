package com.dressme.dressme_database.schema.dto;
 
import java.util.UUID;
 
public record StyleCardEmbeddingResponse(
    UUID id,
    String name,
    float[] embeddingVector   // Los 1536 floats pre-computados
) {}