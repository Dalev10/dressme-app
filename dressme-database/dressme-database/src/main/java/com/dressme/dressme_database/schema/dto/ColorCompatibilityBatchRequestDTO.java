package com.dressme.dressme_database.schema.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ColorCompatibilityBatchRequestDTO(
    @NotEmpty @Valid List<ColorCompatibilityPairRequestDTO> items
) {
}
