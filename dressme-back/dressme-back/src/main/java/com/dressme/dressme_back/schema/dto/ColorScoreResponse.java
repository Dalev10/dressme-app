package com.dressme.dressme_back.schema.dto;

import java.util.List;

public record ColorScoreResponse(
    double score,
    boolean applies,
    List<SlotPairScore> pairScores,
    List<String> warnings
) {
}
