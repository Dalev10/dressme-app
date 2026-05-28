package com.dressme.dressme_database.schema.dto;

import java.util.UUID;

public record TasteSimilarityResponseDTO(
    UUID outfitId,
    double similarity,
    boolean applies
) {
}
