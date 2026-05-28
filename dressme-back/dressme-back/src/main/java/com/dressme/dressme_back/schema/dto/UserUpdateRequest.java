package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserUpdateRequest(
    @JsonProperty("displayName") String displayName,
    @JsonProperty("profilePicture") String profilePicture,
    @JsonProperty("isCalibrated") Boolean isCalibrated,
    @JsonProperty("tasteVector") float[] tasteVector,
    @JsonProperty("sourceType") String sourceType
) {
}