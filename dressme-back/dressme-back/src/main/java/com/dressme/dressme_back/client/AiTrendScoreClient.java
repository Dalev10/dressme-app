package com.dressme.dressme_back.client;
 
import com.dressme.dressme_back.schema.dto.TrendScoreBatchRequest;
import com.dressme.dressme_back.schema.dto.TrendScoreBatchResponse;
import com.dressme.dressme_back.schema.dto.TrendScoreRequest;
import com.dressme.dressme_back.schema.dto.TrendScoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "dressme-ia-trend-client", url = "${app.services.ai-url}")
public interface AiTrendScoreClient {

    @PostMapping("/internal/ai/trend/score")
    TrendScoreResponse scoreTrend(@RequestBody TrendScoreRequest request);

    @PostMapping("/internal/ai/trend/score/batch")
    TrendScoreBatchResponse scoreTrendBatch(@RequestBody TrendScoreBatchRequest request);
}