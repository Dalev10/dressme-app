package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.ColorScoreResponse;
import com.dressme.dressme_back.schema.dto.ScoreComponent;
import com.dressme.dressme_back.schema.dto.ScoreEngineRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreEngineService {

    private static final double COLOR_WEIGHT = 40.0;
    private static final double DRESSCODE_WEIGHT = 20.0;
    private static final double TASTE_WEIGHT = 20.0;
    private static final double TREND_WEIGHT = 20.0;

    private final ColorScoreService colorScoreService;

    public ScoreEngineResponse score(ScoreEngineRequest request) {
        ColorScoreResponse colorScore = colorScoreService.score(request.color());
        boolean colorApplies = colorScore.applies();

        double colorWeight = COLOR_WEIGHT;
        double dresscodeWeight = DRESSCODE_WEIGHT;
        double tasteWeight = TASTE_WEIGHT;
        double trendWeight = TREND_WEIGHT;

        if (!colorApplies) {
            double redistributed = colorWeight / 3.0;
            colorWeight = 0.0;
            dresscodeWeight += redistributed;
            tasteWeight += redistributed;
            trendWeight += redistributed;
        }

        List<ScoreComponent> components = new ArrayList<>();
        components.add(component("color", colorScore.score(), colorWeight));
        components.add(component("dresscode", request.dresscodeScore(), dresscodeWeight));
        components.add(component("taste", request.tasteScore(), tasteWeight));
        components.add(component("trend", request.trendScore(), trendWeight));

        double totalScore = components.stream()
            .mapToDouble(ScoreComponent::weightedScore)
            .sum();

        return new ScoreEngineResponse(
            totalScore,
            colorApplies,
            colorWeight,
            dresscodeWeight,
            tasteWeight,
            trendWeight,
            components,
            colorScore
        );
    }

    private ScoreComponent component(String name, double rawScore, double weight) {
        return new ScoreComponent(name, rawScore, weight, rawScore * weight);
    }
}
