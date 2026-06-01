package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.ColorScoreResponse;
import com.dressme.dressme_back.schema.dto.ScoreComponent;
import com.dressme.dressme_back.schema.dto.ScoreEngineRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;
import com.dressme.dressme_back.schema.dto.TrendScoreResponse;
import com.dressme.dressme_back.service.ColorScoreService;
import com.dressme.dressme_back.service.ScoreEngineService;
import com.dressme.dressme_back.service.TrendScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreEngineServiceImpl implements ScoreEngineService {

    private static final double COLOR_WEIGHT     = 40.0;
    private static final double DRESSCODE_WEIGHT = 20.0;
    private static final double TASTE_WEIGHT     = 20.0;
    private static final double TREND_WEIGHT     = 20.0;

    private final ColorScoreService colorScoreService;
    private final TrendScoreService trendScoreService;

    @Override
    public ScoreEngineResponse score(ScoreEngineRequest request) {
        ColorScoreResponse colorScore = colorScoreService.score(request.color());
        boolean colorApplies = colorScore.applies();

        TrendScoreResponse trendResponse = trendScoreService
            .computeTrendScore(request.outfitEmbeddings());
        boolean trendApplies = trendResponse.applies();

        double colorWeight     = COLOR_WEIGHT;
        double dresscodeWeight = DRESSCODE_WEIGHT;
        double tasteWeight     = TASTE_WEIGHT;
        double trendWeight     = TREND_WEIGHT;

        if (!colorApplies) {
            double redistributed = colorWeight / 3.0;
            colorWeight     = 0.0;
            dresscodeWeight += redistributed;
            tasteWeight     += redistributed;
            trendWeight     += redistributed;
        }

        if (!trendApplies) {
            int activeComponents = (colorApplies ? 1 : 0) + 2;
            if (activeComponents > 0) {
                double redistributedTrend = trendWeight / activeComponents;
                trendWeight = 0.0;
                if (colorApplies) colorWeight += redistributedTrend;
                dresscodeWeight += redistributedTrend;
                tasteWeight     += redistributedTrend;
            }
        }

        List<ScoreComponent> components = new ArrayList<>();
        components.add(component("color",     colorScore.score(),         colorWeight));
        components.add(component("dresscode", request.dresscodeScore(),   dresscodeWeight));
        components.add(component("taste",     request.tasteScore(),       tasteWeight));
        components.add(component("trend",     trendResponse.trendScore(), trendWeight));

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