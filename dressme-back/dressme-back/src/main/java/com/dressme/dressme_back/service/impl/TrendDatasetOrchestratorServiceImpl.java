package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.DatabaseTrendDatasetClient;
import com.dressme.dressme_back.schema.dto.TrendDatasetConfigRequest;
import com.dressme.dressme_back.schema.dto.TrendDatasetConfigResponse;
import com.dressme.dressme_back.service.TrendDatasetOrchestratorService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendDatasetOrchestratorServiceImpl implements TrendDatasetOrchestratorService {

    private final DatabaseTrendDatasetClient databaseTrendDatasetClient;

    @Override
    public TrendDatasetConfigResponse saveDatasetConfig(TrendDatasetConfigRequest request) {
        log.info(
                "TrendDatasetOrchestrator: Delegando persistencia a dressme-database — imageCount={}, model={}",
                request.imageCount(),
                request.modelUsed()
        );

        try {
            TrendDatasetConfigResponse response = databaseTrendDatasetClient.saveTrendConfig(request);

            log.info(
                    "TrendDatasetOrchestrator: Vector persistido exitosamente — id={}, computedAt={}",
                    response.id(),
                    response.computedAt()
            );

            return response;

        } catch (FeignException e) {
            log.error(
                    "TrendDatasetOrchestrator: dressme-database rechazó la persistencia. status={}, body={}",
                    e.status(),
                    e.contentUTF8()
            );
            throw new RuntimeException(
                    "No fue posible persistir el vector de tendencia. dressme-database respondió con " + e.status(),
                    e
            );
        }
    }
}