package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.DressCodeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "dressme-database-dress-code-client", url = "${app.services.database-url}")
public interface DatabaseDressCodeClient {

    @GetMapping("/internal/dress-codes")
    List<DressCodeDTO> getActiveDressCodes();

    @GetMapping("/internal/dress-codes/{id}/compatible")
    List<UUID> getCompatibleDressCodeIds(@PathVariable("id") UUID dressCodeId);

    @GetMapping("/internal/dress-codes/{id}")
    DressCodeDTO getDressCodeById(@PathVariable("id") UUID dressCodeId);
}