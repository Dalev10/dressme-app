package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DatabaseCompatibilityResponse(
    double compatibilityScore
) {
}
