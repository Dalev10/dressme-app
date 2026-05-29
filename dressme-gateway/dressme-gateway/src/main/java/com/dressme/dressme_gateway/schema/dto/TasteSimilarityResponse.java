package com.dressme.dressme_gateway.schema.dto;

import java.util.UUID;

public record TasteSimilarityResponse(
    UUID outfitId,
    double similarity,
    boolean applies
) {
}
