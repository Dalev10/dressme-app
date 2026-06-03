package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Payload que dressme-back envía a dressme-ai para generar candidatos de outfit.
 * Contrato con POST /internal/ai/outfit/generate en dressme-ai.
 */
public record AiOutfitGenerationRequest(

        @NotBlank(message = "El userId es mandatorio")
        @JsonProperty("userId")
        String userId,

        /** Prendas del guardarropa ya filtradas por ocasión y clima */
        @NotNull(message = "El guardarropa no puede ser nulo")
        @Valid // Dispara la validación interna de cada ClothingEmbeddingInfo
        @JsonProperty("wardrobe")
        List<ClothingEmbeddingInfo> wardrobe,

        /** Nombre de la ocasión — para contexto en dressme-ai */
        @NotBlank(message = "La ocasión es requerida para el contexto de la IA")
        @JsonProperty("occasion")
        String occasion,

        /** Nombre del clima — para contexto en dressme-ai */
        @NotBlank(message = "El clima es requerido para el contexto de la IA")
        @JsonProperty("weather")
        String weather,

        /**
         * Vector semántico del dress code seleccionado por el usuario (1536 dims).
         * Null cuando dressCodeId no fue especificado en el request de generación.
         */
        @JsonProperty("dresscodeEmbedding")
        List<Float> dresscodeEmbedding

) {}