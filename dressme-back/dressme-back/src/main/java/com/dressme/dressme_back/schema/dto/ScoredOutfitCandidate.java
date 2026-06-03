package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * Candidato de outfit enriquecido con scores, devuelto por dressme-ai.
 *
 * Espejo del ScoredOutfitCandidate de schemas/outfit.py en dressme-ai.
 *
 * dressme-ai calcula y envía: dresscode_score y outfit_vector.
 * Envía inicializados en 0.0: color_score, taste_score, trend_score, total_score.
 * dressme-back usa este objeto base y completa el scoring con sus servicios 
 * antes de persistir.
 */
public record ScoredOutfitCandidate(

        /**
         * Lista de prendas que componen el outfit candidato.
         */
        @JsonProperty("slots")
        List<CandidateSlot> slots,

        /**
         * Vector semántico del outfit (1536 dims).
         * Calculado por dressme-ai. Usado por TrendScoreService y para persistencia.
         */
        @JsonProperty("outfit_vector")
        List<Float> outfitVector,

        /**
         * Similitud coseno entre outfit_vector y dresscode_embedding [0.0–1.0].
         * Calculado por dressme-ai (DresscodeSimilarityService).
         */
        @JsonProperty("dresscode_score")
        double dresscodeScore,

        /**
         * Inicializado en 0.0 por AI. Calculado en Back por ColorScoreService.
         */
        @JsonProperty("color_score")
        double colorScore,

        /**
         * Inicializado en 0.0 por AI. Calculado en Back por TasteSimilarityService.
         */
        @JsonProperty("taste_score")
        double tasteScore,

        /**
         * Inicializado en 0.0 por AI. Calculado en Back por TrendScoreService.
         */
        @JsonProperty("trend_score")
        double trendScore,

        /**
         * Inicializado en 0.0 por AI. Calculado en Back por ScoreEngineService.
         */
        @JsonProperty("total_score")
        double totalScore

) {
    /**
     * DTO anidado para reflejar el modelo CandidateSlot de Python.
     * (Asegúrate de ajustar "clothing_id" según cómo esté nombrado en el Pydantic de AI).
     */
    public record CandidateSlot(
            @JsonProperty("slot")
            String slot,

            @JsonProperty("clothing_id") 
            UUID clothingId
    ) {}
}