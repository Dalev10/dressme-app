package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO para la actualización de datos del perfil del usuario.
 * Todos los campos son opcionales para soportar el método PATCH.
 */
public record UserUpdateRequest(
    @Size(min = 2, max = 50, message = "Si se envía, el displayName debe tener entre 2 y 50 caracteres")
    String displayName,

    // No forzamos un formato estricto de URL por si en el futuro usamos URIs relativas de un bucket S3
    String profilePictureUrl
) {}