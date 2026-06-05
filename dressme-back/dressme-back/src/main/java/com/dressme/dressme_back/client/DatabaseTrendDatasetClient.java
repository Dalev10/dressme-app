package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.TrendDatasetConfigRequest;
import com.dressme.dressme_back.schema.dto.TrendDatasetConfigResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "dressme-database-trend-dataset-client", url = "${app.services.database-url}")
public interface DatabaseTrendDatasetClient {

    @PostMapping("/internal/trend-dataset/config")
    TrendDatasetConfigResponse saveTrendConfig(@RequestBody TrendDatasetConfigRequest request);
}