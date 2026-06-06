package com.dressme.dressme_back.schema.dto;

import java.util.List;

public record TrendScoreBatchResponse(
    List<TrendScoreResponse> scores
) {}
