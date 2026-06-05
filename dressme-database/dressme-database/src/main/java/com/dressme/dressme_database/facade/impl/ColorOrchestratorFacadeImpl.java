package com.dressme.dressme_database.facade.impl;

import com.dressme.dressme_database.facade.ColorOrchestratorFacade;
import com.dressme.dressme_database.schema.dto.ColorCompatibilityPairRequestDTO;
import com.dressme.dressme_database.schema.dto.ColorResponseDTO;
import com.dressme.dressme_database.schema.dto.CompatibilityResponseDTO;
import com.dressme.dressme_database.service.ColorCompatibilityService;
import com.dressme.dressme_database.service.ColorService;
import com.dressme.dressme_database.util.ColorTheoryUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColorOrchestratorFacadeImpl implements ColorOrchestratorFacade {
    private final ColorService colorService;
    private final ColorCompatibilityService colorCompatibilityService;

    private static final double NEUTRAL_WITH_COLOR_SCORE = 0.75;
    private static final double NEUTRAL_WITH_NEUTRAL_SCORE = 0.60;

    @Override
    public CompatibilityResponseDTO checkCompatibility(int hue1, int saturation1, int lightness1,
                                                       int hue2, int saturation2, int lightness2){

        ColorResponseDTO matchedColor1 = colorService.getClosestColor(hue1, saturation1, lightness1);
        ColorResponseDTO matchedColor2 = colorService.getClosestColor(hue2, saturation2, lightness2);

        double score = resolveNeutralScore(matchedColor1.neutral(), matchedColor2.neutral())
            .orElseGet(() -> colorCompatibilityService
                .getCompatibilityScore(matchedColor1.id(), matchedColor2.id())
                .orElseGet(() -> ColorTheoryUtils.calculateHarmonyScore(matchedColor1.hue(), matchedColor2.hue()))
            );

        return new CompatibilityResponseDTO(matchedColor1, matchedColor2, score);
    }

    @Override
    public List<CompatibilityResponseDTO> checkCompatibilityBatch(List<ColorCompatibilityPairRequestDTO> items) {
        return items.stream()
            .map(item -> checkCompatibility(
                item.hue1(), item.saturation1(), item.lightness1(),
                item.hue2(), item.saturation2(), item.lightness2()
            ))
            .collect(Collectors.toList());
    }

    private Optional<Double> resolveNeutralScore(boolean color1Neutral, boolean color2Neutral) {
        if (color1Neutral && color2Neutral) {
            return Optional.of(NEUTRAL_WITH_NEUTRAL_SCORE);
        }
        if (color1Neutral || color2Neutral) {
            return Optional.of(NEUTRAL_WITH_COLOR_SCORE);
        }
        return Optional.empty();
    }
}
