package com.dressme.dressme_back.schema.dto;
 
import java.util.UUID;
 
public record OnboardingCalibrationResponse(
    UUID userId,
    boolean isCalibrated,
    int tasteVectorDimensions,
    int likesCount,
    int dislikesCount
) {}