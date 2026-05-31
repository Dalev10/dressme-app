package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO Interno: Solo consumible por microservicios de nuestra red privada.
 *
 * Los campos provider y providerId son obligatorios porque siempre se necesitan
 * para buscar al usuario. Los demás campos (email, displayName, etc.) solo
 * se requieren cuando el usuario no existe y hay que crearlo.
 */
public record InternalUserCreateRequest(
    @Email(message = "Formato de email inválido")
    String email,

    String displayName,

    String profilePictureUrl,

    @NotBlank(message = "El provider (GOOGLE/PINTEREST) es obligatorio")
    String provider,

    @NotBlank(message = "El ID del provider es obligatorio")
    String providerId,

    float[] initialTasteVector
) {}