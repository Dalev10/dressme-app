package com.dressme.dressme_back.schema.dto;

public record OnboardingCalibrationResponse(
    String userId,
    boolean calibrated,
    int tasteVectorSize,
    int likesCount,
    int dislikesCount
) {
}
