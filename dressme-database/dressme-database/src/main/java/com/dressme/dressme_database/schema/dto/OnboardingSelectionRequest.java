package com.dressme.dressme_database.schema.dto;
 
import com.dressme.dressme_database.model.UserOnboardingSelection.Reaction;
import jakarta.validation.constraints.NotNull;
 
import java.util.List;
import java.util.UUID;
 
public record OnboardingSelectionRequest(
 
    @NotNull(message = "El userId es obligatorio")
    UUID userId,
 
    @NotNull(message = "Debe incluir al menos una selección")
    List<SelectionItem> selections
 
) {
    public record SelectionItem(
 
        @NotNull(message = "El styleCardId es obligatorio")
        UUID styleCardId,
 
        @NotNull(message = "La reacción es obligatoria (LIKE, DISLIKE, SKIP)")
        Reaction reaction
 
    ) {}
}