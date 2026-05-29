package com.dressme.dressme_back.schema.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserProfileResponse(
    UUID id,
    String email,
    String displayName,
    String profilePicture,
    @JsonProperty("isCalibrated") boolean isCalibrated
) {}