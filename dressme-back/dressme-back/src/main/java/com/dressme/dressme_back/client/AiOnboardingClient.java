package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.ComputeTasteVectorRequest;
import com.dressme.dressme_back.schema.dto.ComputeTasteVectorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "dressme-ia-client", url = "${app.services.ai-url}")
public interface AiOnboardingClient {

    @PostMapping("/internal/ai/onboarding/compute-taste-vector")
    ComputeTasteVectorResponse computeTasteVector(@RequestBody ComputeTasteVectorRequest computeTasteVectorRequest);
}
