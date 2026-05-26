package com.dressme.dressme_database.facade;

import com.dressme.dressme_database.schema.dto.ColorCompatibilityPairRequestDTO;
import com.dressme.dressme_database.schema.dto.CompatibilityResponseDTO;

import java.util.List;

public interface ColorOrchestratorFacade {
    CompatibilityResponseDTO checkCompatibility(int h1, int s1, int l1,
                                                int h2, int s2, int l2);

    List<CompatibilityResponseDTO> checkCompatibilityBatch(List<ColorCompatibilityPairRequestDTO> items);
}
