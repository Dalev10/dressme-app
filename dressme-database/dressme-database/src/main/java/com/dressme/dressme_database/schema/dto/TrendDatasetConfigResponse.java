package com.dressme.dressme_database.schema.dto;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta que dressme-database devuelve tras persistir el vector.
 * Incluye el ID, avgVector, imageCount y computed_at para que AI pueda consumirlo.
 */
public record TrendDatasetConfigResponse(
    UUID id,
    List<Float> avgVector,
    int imageCount,
    String modelUsed,
    String description,
    LocalDateTime computedAt
) {}