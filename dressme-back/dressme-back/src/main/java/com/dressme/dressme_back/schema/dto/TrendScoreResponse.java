package com.dressme.dressme_back.schema.dto;
 
/**
 * Respuesta del servicio AI para el trend score de un outfit.
 *
 * trendScore    → Similitud coseno entre outfit_vector y avg_vector del dataset [0, 1].
 * applies       → False si el dataset no está cargado en DB (redistribuir peso).
 * datasetImages → Imágenes del dataset usadas para calcular el avg_vector.
 */
public record TrendScoreResponse(
    double trendScore,
    boolean applies,
    int datasetImages
) {}