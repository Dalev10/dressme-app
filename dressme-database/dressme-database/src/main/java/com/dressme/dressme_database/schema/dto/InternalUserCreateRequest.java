package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO Interno: Solo consumible por microservicios de nuestra red privada.
 */
public record InternalUserCreateRequest(
    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "Formato de email inválido")
    String email,

    @NotBlank(message = "El displayName es obligatorio")
    String displayName,

    String profilePictureUrl,

    @NotBlank(message = "El provider (GOOGLE/PINTEREST) es obligatorio")
    String provider,

    @NotBlank(message = "El ID del provider es obligatorio")
    String providerId,

    @NotNull(message = "El vector de gusto inicial es obligatorio")
    @Size(min = 1536, max = 1536, message = "El vector debe tener exactamente 1536 dimensiones")
    float[] initialTasteVector
) {}