package com.dressme.dressme_gateway.schema.dto;

public record OnboardingCalibrationResponse(
    String userId,
    boolean calibrated,
    int tasteVectorSize,
    int likesCount,
    int dislikesCount
) {}