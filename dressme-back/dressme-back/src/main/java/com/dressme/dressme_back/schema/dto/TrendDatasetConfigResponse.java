package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Espejo del TrendDatasetConfigResponse de dressme-database.
 *
 * Se agrega avgVector para que TrendScoreServiceImpl pueda consumir
 * el vector del dataset directamente desde este DTO si lo necesita.
 *
 * @JsonProperty("avgVector") garantiza que Jackson deserialice
 * correctamente la clave camelCase que devuelve dressme-database,
 * independientemente de la configuración global de naming strategy.
 */
public record TrendDatasetConfigResponse(
        UUID id,
        @JsonProperty("avgVector") List<Float> avgVector,
        int imageCount,
        String modelUsed,
        String description,
        LocalDateTime computedAt
) {}