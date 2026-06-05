package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Payload que dressme-back envía a dressme-database para persistir el
 * embedding vectorial de una prenda.
 *
 * Contrato con PATCH /internal/wardrobe/{clothingId}/embedding.
 */
public record ClothingEmbeddingUpdateRequest(

        @JsonProperty("clothingId")
        UUID clothingId,

        @JsonProperty("embeddingVector")
        List<Float> embeddingVector

) {}
