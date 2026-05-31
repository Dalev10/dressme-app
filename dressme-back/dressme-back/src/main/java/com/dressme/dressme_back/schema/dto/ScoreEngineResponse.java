package com.dressme.dressme_back.schema.dto;

import java.util.List;

public record ScoreEngineResponse(
    double totalScore,
    boolean colorApplies,
    double colorWeight,
    double dresscodeWeight,
    double tasteWeight,
    double trendWeight,
    List<ScoreComponent> components,
    ColorScoreResponse colorDetails
) {
}
