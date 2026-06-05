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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreEngineServiceImpl implements ScoreEngineService {

    @Value("${app.score-engine.weights.color:40.0}")
    private double colorBaseWeight;

    @Value("${app.score-engine.weights.dresscode:20.0}")
    private double dresscodeBaseWeight;

    @Value("${app.score-engine.weights.taste:20.0}")
    private double tasteBaseWeight;

    @Value("${app.score-engine.weights.trend:20.0}")
    private double trendBaseWeight;

    private final ColorScoreService colorScoreService;
    private final TrendScoreService trendScoreService;

    @Override
    public ScoreEngineResponse score(ScoreEngineRequest request) {
        ColorScoreResponse colorScore = colorScoreService.score(request.color());
        boolean colorApplies = colorScore.applies();

        TrendScoreResponse trendResponse = trendScoreService
            .computeTrendScore(request.outfitEmbeddings());
        boolean trendApplies = trendResponse.applies();

        double colorWeight     = colorBaseWeight;
        double dresscodeWeight = dresscodeBaseWeight;
        double tasteWeight     = tasteBaseWeight;
        double trendWeight     = trendBaseWeight;

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

    @Override
    public ScoreEngineResponse scoreFromComponents(
            ColorScoreResponse precomputedColor,
            double dresscodeScore,
            double tasteScore,
            TrendScoreResponse precomputedTrend) {

        boolean colorApplies = precomputedColor.applies();
        boolean trendApplies = precomputedTrend.applies();

        double colorWeight     = colorBaseWeight;
        double dresscodeWeight = dresscodeBaseWeight;
        double tasteWeight     = tasteBaseWeight;
        double trendWeight     = trendBaseWeight;

        if (!colorApplies) {
            double redistributed = colorWeight / 3.0;
            colorWeight      = 0.0;
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
        components.add(component("color",     precomputedColor.score(),        colorWeight));
        components.add(component("dresscode", dresscodeScore,                  dresscodeWeight));
        components.add(component("taste",     tasteScore,                      tasteWeight));
        components.add(component("trend",     precomputedTrend.trendScore(),   trendWeight));

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
                precomputedColor
        );
    }

    private ScoreComponent component(String name, double rawScore, double weight) {
        return new ScoreComponent(name, rawScore, weight, rawScore * weight);
    }
}