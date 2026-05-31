package com.dressme.dressme_gateway.schema.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta del endpoint de validación de tokens de Google.
 *
 * Referencia: GET https://oauth2.googleapis.com/tokeninfo?id_token={token}
 *
 * Google devuelve los campos con snake_case y nombres propios ("sub", "picture"),
 * por lo que cada campo requiere su @JsonProperty correspondiente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfoResponse(

    // Identificador único del usuario en Google (el "sub" del JWT estándar)
    @JsonProperty("sub")
    String providerId,

    @JsonProperty("email")
    String email,

    // Nombre completo del usuario
    @JsonProperty("name")
    String displayName,

    @JsonProperty("picture")
    String profilePictureUrl,

    // Garantiza que el email es real y verificado por Google
    @JsonProperty("email_verified")
    String emailVerified,

    // Debe coincidir con tu Google Client ID para validar que el token es para tu app
    @JsonProperty("aud")
    String audience

) {}
