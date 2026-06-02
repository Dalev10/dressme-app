package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.ScoreEngineOrchestrationRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;

public interface ScoreEngineOrchestratorService {
    ScoreEngineResponse score(ScoreEngineOrchestrationRequest request);
}