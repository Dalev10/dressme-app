package com.dressme.dressme_back.schema.dto;

import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String displayName,
    String profilePicture,
    boolean isCalibrated
) {}