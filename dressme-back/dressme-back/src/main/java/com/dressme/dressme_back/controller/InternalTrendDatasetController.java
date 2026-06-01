package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.schema.dto.TrendDatasetConfigRequest;
import com.dressme.dressme_back.schema.dto.TrendDatasetConfigResponse;
import com.dressme.dressme_back.service.TrendDatasetOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/trend-dataset")
@RequiredArgsConstructor
@Slf4j
public class InternalTrendDatasetController {

    private final TrendDatasetOrchestratorService trendDatasetOrchestratorService;

    @PostMapping("/config")
    public ResponseEntity<TrendDatasetConfigResponse> saveConfig(
            @Valid @RequestBody TrendDatasetConfigRequest request
    ) {
        log.info(
                "Back-TrendDataset: Recibida solicitud desde script Python — imageCount={}",
                request.imageCount()
        );

        TrendDatasetConfigResponse response = trendDatasetOrchestratorService.saveDatasetConfig(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}