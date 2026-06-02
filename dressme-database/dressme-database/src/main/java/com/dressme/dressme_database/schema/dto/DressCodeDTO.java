package com.dressme.dressme_database.schema.dto;

import java.util.UUID;

public record DressCodeDTO(
    UUID id,
    String name,
    String description,
    float[] embeddingVector,
    boolean isActive
) {}