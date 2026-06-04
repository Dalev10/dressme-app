package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta de dressme-ai tras vectorizar una prenda.
 * Contrato con POST /ai/outfit/embed-clothing.
 */
public record ClothingEmbeddingResponse(

        @JsonProperty("clothingId")
        UUID clothingId,

        @JsonProperty("embeddingVector")
        List<Float> embeddingVector

) {}
