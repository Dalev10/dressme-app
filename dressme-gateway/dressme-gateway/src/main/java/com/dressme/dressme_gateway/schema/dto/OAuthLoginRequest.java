package com.dressme.dressme_gateway.schema.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
    @NotBlank(message = "El proveedor es obligatorio (ej. GOOGLE, PINTEREST)")
    String provider,
    
    @NotBlank(message = "El token de acceso o ID token provisto por el frontend es obligatorio")
    String token
) {}