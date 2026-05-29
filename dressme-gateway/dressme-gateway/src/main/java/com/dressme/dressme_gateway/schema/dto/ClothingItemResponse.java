package com.dressme.dressme_gateway.schema.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta resumida de una prenda del guardarropa.
 * Usada en el listado general (grid) para mantener payloads ligeros.
 *
 * categoryId / categoryName son null cuando isProcessed = false
 * (la IA aún no terminó el análisis). El frontend debe manejar
 * este estado mostrando un indicador de "Analizando…".
 */
public record ClothingItemResponse(

    UUID id,
    UUID userId,
    String imageUrl,

    /** null si la IA aún no procesó la prenda */
    UUID categoryId,
    String categoryName,

    /**
     * false = recién subida, pendiente de análisis Vision.
     * true  = categoría, colores y ocasión ya asignados.
     */
    boolean isProcessed,

    LocalDateTime createdAt

) {}
