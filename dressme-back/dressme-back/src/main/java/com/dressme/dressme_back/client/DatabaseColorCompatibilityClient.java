package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.DatabaseCompatibilityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "dressme-database-color-client", url = "${app.services.database-url}")
public interface DatabaseColorCompatibilityClient {

    @GetMapping("/internal/colors/compatibility")
    DatabaseCompatibilityResponse checkCompatibility(
        @RequestParam("hue1") int hue1,
        @RequestParam("saturation1") int saturation1,
        @RequestParam("lightness1") int lightness1,
        @RequestParam("hue2") int hue2,
        @RequestParam("saturation2") int saturation2,
        @RequestParam("lightness2") int lightness2
    );
}
