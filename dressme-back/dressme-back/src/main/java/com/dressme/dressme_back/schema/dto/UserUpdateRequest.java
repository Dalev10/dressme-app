package com.dressme.dressme_back.schema.dto;

public record UserUpdateRequest(
    String email,
    String name,
    Boolean calibrated,
    float[] tasteVector,
    String source
) {
}
