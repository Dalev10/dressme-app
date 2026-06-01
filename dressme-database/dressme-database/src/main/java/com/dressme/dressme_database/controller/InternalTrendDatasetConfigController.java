package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.TrendDatasetConfigRequest;
import com.dressme.dressme_database.schema.dto.TrendDatasetConfigResponse;
import com.dressme.dressme_database.service.TrendDatasetConfigService;
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
public class InternalTrendDatasetConfigController {

    private final TrendDatasetConfigService trendDatasetConfigService;

    @PostMapping("/config")
    public ResponseEntity<TrendDatasetConfigResponse> saveConfig(
            @Valid @RequestBody TrendDatasetConfigRequest request
    ) {
        log.info(
                "Database-TrendDataset: Recibida solicitud de persistencia — imageCount={}",
                request.imageCount()
        );

        TrendDatasetConfigResponse response = trendDatasetConfigService.save(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}