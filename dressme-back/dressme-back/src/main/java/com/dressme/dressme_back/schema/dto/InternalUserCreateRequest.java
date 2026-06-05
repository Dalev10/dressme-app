package com.dressme.dressme_back.schema.dto;

public record InternalUserCreateRequest(
    String email,
    String displayName,
    String profilePictureUrl,
    String provider,
    String providerId,
    float[] initialTasteVector // Aquí inyectaremos los 1536 ceros
) {}