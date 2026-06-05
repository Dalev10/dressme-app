package com.dressme.dressme_database.schema.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {
}
