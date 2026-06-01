package com.dressme.dressme_database.schema.dto;
 
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
 
/**
 * Payload que recibe dressme-database desde dressme-back para persistir
 * el vector promedio del dataset de moda actual.
 *
 * avgVector    → Vector de 1536 floats (promedio de embeddings del dataset).
 * imageCount   → Número de imágenes procesadas (indicador de confianza).
 * modelUsed    → Modelo de IA usado para generar los embeddings.
 * description  → Descripción libre para trazabilidad.
 */
public record TrendDatasetConfigRequest(
    @NotNull
    @Size(min = 1536, max = 1536, message = "avgVector debe tener exactamente 1536 dimensiones")
    float[] avgVector,
 
    @Min(value = 1, message = "imageCount debe ser al menos 1")
    int imageCount,
 
    @NotBlank
    String modelUsed,
 
    String description
) {}