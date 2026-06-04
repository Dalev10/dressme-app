package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Payload que dressme-back envía a dressme-ai para vectorizar una prenda.
 * Contrato con POST /ai/outfit/embed-clothing.
 *
 * dressme-ai construye el texto semántico:
 *   "{category} {style} {color} para {weather} y {occasion}"
 * y genera un embedding de 1536 dims con gemini-embedding-001.
 */
public record ClothingEmbeddingRequest(

        @NotNull
        @JsonProperty("clothingId")
        UUID clothingId,

        @NotBlank
        @JsonProperty("category")
        String category,

        @NotBlank
        @JsonProperty("style")
        String style,

        @NotBlank
        @JsonProperty("color")
        String color,

        @NotBlank
        @JsonProperty("weather")
        String weather,

        @NotBlank
        @JsonProperty("occasion")
        String occasion

) {}
