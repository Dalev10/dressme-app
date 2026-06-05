package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.TrendDatasetConfigRequest;
import com.dressme.dressme_back.schema.dto.TrendDatasetConfigResponse;

public interface TrendDatasetOrchestratorService {

    TrendDatasetConfigResponse saveDatasetConfig(TrendDatasetConfigRequest request);
}