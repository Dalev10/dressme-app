package com.dressme.dressme_gateway.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Respuesta del endpoint de login con token JWT incluido.
 * Se devuelve tras validar OAuth y crear/recuperar usuario.
 */
@Data
@AllArgsConstructor
public class AuthLoginResponse {
    private UUID id;
    private String email;
    private String displayName;
    private String profilePicture;
    @JsonProperty("isCalibrated")
    private boolean isCalibrated;
    private String token;
}
