package com.dressme.dressme_gateway.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mapeo estricto de la respuesta de la API de validación de Google.
 */
public record GoogleUserInfoResponse(
    @JsonProperty("sub") String providerId,       // El ID único e inmutable del usuario en Google
    @JsonProperty("email") String email,
    @JsonProperty("name") String displayName,
    @JsonProperty("picture") String profilePictureUrl,
    @JsonProperty("email_verified") String emailVerified
) {}