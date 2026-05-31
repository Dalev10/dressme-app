package com.dressme.dressme_back.schema.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ColorScoreRequest(
    @NotEmpty @Valid List<SlotColorInput> items
) {
}
