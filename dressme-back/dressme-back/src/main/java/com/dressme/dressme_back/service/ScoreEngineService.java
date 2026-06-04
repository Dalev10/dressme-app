package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.ColorScoreResponse;
import com.dressme.dressme_back.schema.dto.ScoreEngineRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;
import com.dressme.dressme_back.schema.dto.TrendScoreResponse;

public interface ScoreEngineService {

    /** Calcula scores llamando internamente a ColorScoreService y TrendScoreService. */
    ScoreEngineResponse score(ScoreEngineRequest request);

    /**
     * Calcula totalScore con scores ya pre-computados — sin llamadas HTTP adicionales.
     * Usado por OutfitOrchestratorService para evitar re-calcular color y trend
     * cuando ya se computaron en paralelo antes de llamar aquí.
     */
    ScoreEngineResponse scoreFromComponents(
            ColorScoreResponse precomputedColor,
            double dresscodeScore,
            double tasteScore,
            TrendScoreResponse precomputedTrend
    );
}