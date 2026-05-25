package com.dressme.dressme_database.facade.impl;

import com.dressme.dressme_database.facade.ColorOrchestratorFacade;
import com.dressme.dressme_database.schema.dto.ColorResponseDTO;
import com.dressme.dressme_database.schema.dto.CompatibilityResponseDTO;
import com.dressme.dressme_database.service.ColorCompatibilityService;
import com.dressme.dressme_database.service.ColorService;
import com.dressme.dressme_database.util.ColorTheoryUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ColorOrchestratorFacadeImpl implements ColorOrchestratorFacade {
    private final ColorService colorService;
    private final ColorCompatibilityService colorCompatibilityService;

    @Override
    public CompatibilityResponseDTO checkCompatibility(int hue1, int saturation1, int lightness1,
                                                       int hue2, int saturation2, int lightness2){

        ColorResponseDTO matchedColor1 = colorService.getClosestColor(hue1, saturation1, lightness1);
        ColorResponseDTO matchedColor2 = colorService.getClosestColor(hue2, saturation2, lightness2);

        Optional<Double> storedScore = colorCompatibilityService
            .getCompatibilityScore(matchedColor1.id(), matchedColor2.id());

        double score = storedScore.orElseGet(
            () -> ColorTheoryUtils.calculateHarmonyScore(matchedColor1.hue(), matchedColor2.hue())
        );

        return new CompatibilityResponseDTO(matchedColor1, matchedColor2, score);
    }
}
