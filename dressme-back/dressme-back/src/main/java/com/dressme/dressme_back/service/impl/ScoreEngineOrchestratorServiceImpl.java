package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.ScoreEngineOrchestrationRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;
import com.dressme.dressme_back.service.DressCodeScoreService;
import com.dressme.dressme_back.service.ScoreEngineOrchestratorService;
import com.dressme.dressme_back.service.ScoreEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreEngineOrchestratorServiceImpl implements ScoreEngineOrchestratorService {

    private final DressCodeScoreService dressCodeScoreService;
    private final ScoreEngineService scoreEngineService;

    @Override
    public ScoreEngineResponse score(ScoreEngineOrchestrationRequest request) {
        double dresscodeScore = dressCodeScoreService.score(request.outfitId(), request.userId());

        ScoreEngineRequest scoreEngineRequest = new ScoreEngineRequest(
                request.color(),
                dresscodeScore,
                request.tasteScore(),
                request.outfitEmbeddings()
        );

        return scoreEngineService.score(scoreEngineRequest);
    }
}