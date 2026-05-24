package com.dressme.dressme_back.schema.dto;
 
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
 
public record OnboardingSelectionRequest(
 
    @NotNull(message = "El userId es obligatorio")
    UUID userId,
 
    @NotEmpty(message = "Debe incluir al menos una selección")
    List<SelectionItem> selections
 
) {
    public record SelectionItem(
        @NotNull UUID styleCardId,
        @NotNull String reaction   // "LIKE" | "DISLIKE" | "SKIP"
    ) {}
}