package com.dressme.dressme_back.schema.dto;

import java.util.UUID;

public record DressCodeDTO(
    UUID id,
    String name,
    String description,
    float[] embeddingVector,
    boolean isActive
) {}