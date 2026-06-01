package com.dressme.dressme_back.client;
 
import com.dressme.dressme_back.schema.dto.TrendScoreRequest;
import com.dressme.dressme_back.schema.dto.TrendScoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
 
/**
 * Feign client que llama a dressme-ai para obtener el trend score de un outfit.
 */
@FeignClient(name = "dressme-ia-trend-client", url = "${app.services.ai-url}")
public interface AiTrendScoreClient {
 
    @PostMapping("/internal/ai/trend/score")
    TrendScoreResponse scoreTrend(@RequestBody TrendScoreRequest request);
}