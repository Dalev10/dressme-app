package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.DatabaseOnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;
import com.dressme.dressme_back.schema.dto.StyleCardEmbeddingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "dressme-database-onboarding-client", url = "${app.services.database-url}")
public interface DatabaseOnboardingClient {

    @GetMapping("/internal/onboarding/style-cards")
    List<StyleCardDTO> getStyleCards();

    @PostMapping("/internal/onboarding/style-cards/embeddings")
    List<StyleCardEmbeddingResponse> getEmbeddings(@RequestBody List<UUID> ids);

    @PostMapping("/internal/onboarding/selections")
    void saveSelections(@RequestBody DatabaseOnboardingSelectionRequest request);
}
