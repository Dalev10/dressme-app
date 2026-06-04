package com.dressme.dressme_database.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Payload para persistir el embedding vectorial de una prenda.
 *
 * Recibido desde dressme-back tras llamar a dressme-ai /embed-clothing.
 * Guarda el vector en tbl_clothes.embedding_vector y marca
 * is_embedding_stale = false.
 */
public record ClothingEmbeddingUpdateRequest(

        @NotNull
        @JsonProperty("clothingId")
        UUID clothingId,

        @NotEmpty
        @Size(min = 1536, max = 1536, message = "El embedding debe tener exactamente 1536 dimensiones")
        @JsonProperty("embeddingVector")
        List<Float> embeddingVector

) {}
