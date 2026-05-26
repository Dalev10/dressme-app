package com.dressme.dressme_back.schema.dto;

import java.util.List;
import java.util.UUID;

public record DatabaseOnboardingSelectionRequest(
    UUID userId,
    List<SelectionItem> selections
) {
    public record SelectionItem(
        UUID styleCardId,
        String reaction
    ) {}
}
