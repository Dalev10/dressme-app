package com.dressme.dressme_back.schema.dto;

public record StandardizedUserProviderInfo(
    String provider,
    String providerId,
    String email,
    String displayName,
    String profilePictureUrl
) {}