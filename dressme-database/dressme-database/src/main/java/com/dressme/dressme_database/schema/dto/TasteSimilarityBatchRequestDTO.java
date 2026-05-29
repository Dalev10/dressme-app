package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record TasteSimilarityBatchRequestDTO(
    @NotNull UUID userId,
    @NotEmpty List<UUID> outfitIds
) {
}
