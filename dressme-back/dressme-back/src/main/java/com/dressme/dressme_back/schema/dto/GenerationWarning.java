package com.dressme.dressme_back.schema.dto;

import java.util.List;
import java.util.UUID;

public record GenerationWarning(
        String code,
        String message,
        List<UUID> clothingIds
) {
}