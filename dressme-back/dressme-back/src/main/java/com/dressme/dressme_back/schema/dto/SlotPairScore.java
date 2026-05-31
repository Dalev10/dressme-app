package com.dressme.dressme_back.schema.dto;

public record SlotPairScore(
    Slot slotA,
    Slot slotB,
    double compatibilityScore,
    double weight,
    double weightedScore
) {
}
