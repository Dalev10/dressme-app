package com.dressme.dressme_database.schema.dto;

import java.util.UUID;

/**
 * DTO de salida: Seguro para ser expuesto y enviado de vuelta al Gateway/Frontend.
 */
public record UserProfileResponse(
    UUID id,
    String email,
    String displayName,
    String profilePicture,
    boolean isCalibrated // Bandera útil para que el Frontend sepa si debe mostrar el onboarding
) {}