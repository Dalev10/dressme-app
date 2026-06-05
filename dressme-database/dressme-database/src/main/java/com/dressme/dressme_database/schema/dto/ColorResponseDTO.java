package com.dressme.dressme_database.schema.dto;

import java.util.UUID;

public record ColorResponseDTO(
    UUID id,
    String name,
    Integer hue,
    Integer saturation,
    Integer lightness,
    boolean neutral
) {
}
