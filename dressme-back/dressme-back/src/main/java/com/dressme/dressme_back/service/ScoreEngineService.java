package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.ScoreEngineRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;

public interface ScoreEngineService {
    ScoreEngineResponse score(ScoreEngineRequest request);
}