package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Respuesta de dressme-ai al endpoint POST /internal/ai/outfit/generate.
 *
 * Contiene los candidatos de outfit enriquecidos con dresscode_score
 * y outfit_vector. dressme-back toma estos candidatos y completa el
 * scoring (color, taste, trend) antes de persistir y rankear.
 */
public record AiOutfitGenerationResponse(

        /**
         * Candidatos generados por dressme-ai.
         * * NOTA DE CONTRATO: Cada candidato contiene una LISTA de CandidateSlot (list[CandidateSlot]),
         * reflejando fielmente la estructura de datos nativa enviada por el microservicio de IA.
         */
        @NotNull(message = "La lista de candidatos no puede ser nula")
        @Valid
        @JsonProperty("candidates")
        List<ScoredOutfitCandidate> candidates,

        /** * Total de candidatos generados en el motor de IA antes de cualquier filtro.
         * Mapeado automáticamente desde 'total_candidates' gracias a CamelModel.
         */
        @Min(value = 0, message = "El total de candidatos no puede ser un valor negativo")
        @JsonProperty("totalCandidates")
        int totalCandidates,

        /**
         * Cantidad de prendas del guardarropa que fueron descartadas en dressme-ai 
         * debido a la ausencia de embedding o por poseer un slot inválido.
         * Utilizado para tareas de trazabilidad y debugging del flujo.
         */
        @Min(value = 0, message = "El conteo de prendas omitidas no puede ser un valor negativo")
        @JsonProperty("skippedClothing")
        int skippedClothing

) {}