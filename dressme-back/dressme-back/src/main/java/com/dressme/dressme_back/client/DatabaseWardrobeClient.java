package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.OutfitDressCodeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "dressme-database-wardrobe-client", url = "${app.services.database-url}")
public interface DatabaseWardrobeClient {

    @GetMapping("/internal/wardrobe/{outfitId}/dress-code")
    OutfitDressCodeResponse getDressCode(@PathVariable("outfitId") UUID outfitId);
}