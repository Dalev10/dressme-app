package com.dressme.dressme_database.schema.dto;
 
import java.time.LocalDateTime;
import java.util.UUID;
 
/**
 * Respuesta que dressme-database devuelve tras persistir el vector.
 * Incluye el ID y computed_at generados por Hibernate para confirmación.
 */
public record TrendDatasetConfigResponse(
    UUID id,
    int imageCount,
    String modelUsed,
    String description,
    LocalDateTime computedAt
) {}