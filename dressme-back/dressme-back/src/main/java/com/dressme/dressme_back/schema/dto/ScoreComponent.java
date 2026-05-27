package com.dressme.dressme_back.schema.dto;

public record ScoreComponent(
    String name,
    double rawScore,
    double weight,
    double weightedScore
) {
}
