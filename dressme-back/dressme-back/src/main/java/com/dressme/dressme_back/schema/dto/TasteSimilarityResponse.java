package com.dressme.dressme_back.schema.dto;

import java.util.UUID;

public record TasteSimilarityResponse(
    UUID outfitId,
    double similarity,
    boolean applies
) {
}
